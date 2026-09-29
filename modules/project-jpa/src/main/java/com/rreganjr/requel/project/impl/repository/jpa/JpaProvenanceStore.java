/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.requel.project.impl.repository.jpa;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.EntitySourceLink;
import com.rreganjr.requel.project.ExternalSource;
import com.rreganjr.requel.project.ProvenanceStore;
import com.rreganjr.requel.project.SourceLinkRelation;
import com.rreganjr.requel.project.impl.EntitySourceLinkImpl;
import com.rreganjr.requel.project.impl.ExternalSourceImpl;

/**
 * Issue #272: the JPA {@link ProvenanceStore}.
 */
@Repository("provenanceStore")
@Scope("singleton")
@Transactional(propagation = Propagation.REQUIRED)
public class JpaProvenanceStore implements ProvenanceStore {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public RecordedSource recordSource(SourceSpec spec, User by) {
		Objects.requireNonNull(spec, "spec");
		String system = ProvenanceStore.normalizeSystem(spec.system());
		String externalId = spec.externalId() == null ? null : spec.externalId().strip();
		if (spec.projectId() == null || system == null || system.isEmpty() || externalId == null
				|| externalId.isEmpty()) {
			throw new IllegalArgumentException("a source needs a project, a system and an externalId");
		}
		if (system.length() > MAX_SYSTEM_LENGTH) {
			throw new IllegalArgumentException("system is longer than " + MAX_SYSTEM_LENGTH);
		}
		if (externalId.length() > MAX_EXTERNAL_ID_LENGTH) {
			throw new IllegalArgumentException("externalId is longer than " + MAX_EXTERNAL_ID_LENGTH);
		}
		ProvenanceStore.validateLocator(spec.locatorType(), spec.locator());
		ExternalSourceImpl source = (ExternalSourceImpl) findSource(spec.projectId(), system,
				externalId).orElse(null);
		boolean created = source == null;
		boolean changed = false;
		if (created) {
			source = new ExternalSourceImpl(spec.projectId(), system, externalId, by);
			entityManager.persist(source);
		}
		if (spec.locator() != null) {
			source.setLocatorType(spec.locatorType());
			source.setLocator(spec.locator().strip());
		}
		if (spec.title() != null) {
			source.setTitle(truncate(spec.title().strip(), MAX_TITLE_LENGTH));
		}
		if (spec.contentHash() != null && !spec.contentHash().isBlank()) {
			String hash = spec.contentHash().strip();
			changed = !created && source.getContentHash() != null
					&& !source.getContentHash().equals(hash);
			source.setContentHash(hash);
		}
		source.setLastIngestedAt(new Date());
		return new RecordedSource(source, created, changed);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Optional<ExternalSource> findSource(Long projectId, String system, String externalId) {
		if (projectId == null || system == null || externalId == null) {
			return Optional.empty();
		}
		// Exact match on the external id: it has to round-trip to the source system (#255's tag
		// lower-casing is one of the reasons tags were not enough).
		return entityManager.createQuery("select s from ExternalSourceImpl s "
				+ "where s.projectId = :projectId and s.system = :system and s.externalId = :externalId",
				ExternalSourceImpl.class)
				.setParameter("projectId", projectId)
				.setParameter("system", ProvenanceStore.normalizeSystem(system))
				.setParameter("externalId", externalId.strip())
				.getResultList().stream()
				// MySQL's default collation compares case-insensitively; keep the exact one.
				.filter(s -> s.getExternalId().equals(externalId.strip()))
				.<ExternalSource>map(s -> s).findFirst();
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<ExternalSource> listSources(Long projectId) {
		if (projectId == null) {
			return Collections.emptyList();
		}
		return List.copyOf(entityManager.createQuery("select s from ExternalSourceImpl s "
				+ "where s.projectId = :projectId order by s.system, s.externalId, s.id",
				ExternalSourceImpl.class).setParameter("projectId", projectId).getResultList());
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<EntitySourceLink> linksForTarget(String targetType, Long targetId) {
		if (targetType == null || targetId == null) {
			return Collections.emptyList();
		}
		return List.copyOf(entityManager.createQuery("select l from EntitySourceLinkImpl l "
				+ "where l.targetType = :targetType and l.targetId = :targetId "
				+ "order by l.source.system, l.source.externalId, l.fragmentKey, l.id",
				EntitySourceLinkImpl.class).setParameter("targetType", targetType)
				.setParameter("targetId", targetId).getResultList());
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<EntitySourceLink> linksForSource(Long sourceId) {
		if (sourceId == null) {
			return Collections.emptyList();
		}
		return List.copyOf(entityManager.createQuery("select l from EntitySourceLinkImpl l "
				+ "where l.source.id = :sourceId order by l.fragmentKey, l.targetType, l.targetId",
				EntitySourceLinkImpl.class).setParameter("sourceId", sourceId).getResultList());
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<EntitySourceLink> linksForFragment(Long sourceId, SourceLinkRelation relation,
			String fragment, String targetType) {
		if (sourceId == null || relation == null) {
			return Collections.emptyList();
		}
		String key = ProvenanceStore.fragmentKey(fragment);
		String jpql = "select l from EntitySourceLinkImpl l where l.source.id = :sourceId "
				+ "and l.relation = :relation and l.fragmentKey = :fragmentKey"
				+ (targetType == null ? "" : " and l.targetType = :targetType")
				+ " order by l.targetType, l.targetId";
		var query = entityManager.createQuery(jpql, EntitySourceLinkImpl.class)
				.setParameter("sourceId", sourceId).setParameter("relation", relation)
				.setParameter("fragmentKey", key);
		if (targetType != null) {
			query.setParameter("targetType", targetType);
		}
		// Fragments are matched exactly, like external ids.
		return List.copyOf(query.getResultList().stream()
				.filter(l -> key.equals(l.getFragmentKey())).toList());
	}

	@Override
	public EntitySourceLink link(LinkSpec spec, User by) {
		Objects.requireNonNull(spec, "spec");
		if (spec.source() == null || spec.relation() == null || spec.targetType() == null
				|| spec.targetId() == null) {
			throw new IllegalArgumentException("a link needs a source, a relation and a target");
		}
		String fragment = ProvenanceStore.normalizeFragment(spec.fragment());
		if (fragment != null && fragment.length() > MAX_FRAGMENT_LENGTH) {
			throw new IllegalArgumentException("fragment is longer than " + MAX_FRAGMENT_LENGTH);
		}
		EntitySourceLinkImpl link = (EntitySourceLinkImpl) find(spec.source().getId(),
				spec.relation(), spec.targetType(), spec.targetId(), fragment);
		if (link == null) {
			ExternalSourceImpl source = entityManager.find(ExternalSourceImpl.class,
					spec.source().getId());
			link = new EntitySourceLinkImpl(source, spec.relation(), spec.targetType(),
					spec.targetId(), fragment, by);
			entityManager.persist(link);
		}
		if (spec.fragmentHash() != null) {
			link.setFragmentHash(spec.fragmentHash());
		}
		if (spec.sourceHashSeen() != null) {
			link.setSourceHashSeen(spec.sourceHashSeen());
		}
		if (spec.entityFingerprint() != null) {
			link.setEntityFingerprint(spec.entityFingerprint());
		}
		link.setIngestedAt(new Date());
		return link;
	}

	@Override
	public void recordFingerprint(Long linkId, String entityFingerprint) {
		EntitySourceLinkImpl link = linkId == null ? null
				: entityManager.find(EntitySourceLinkImpl.class, linkId);
		if (link != null) {
			link.setEntityFingerprint(entityFingerprint);
		}
	}

	@Override
	public void restoreLastIngestedAt(Long sourceId, Date lastIngestedAt) {
		ExternalSourceImpl source = sourceId == null ? null
				: entityManager.find(ExternalSourceImpl.class, sourceId);
		if (source != null && lastIngestedAt != null) {
			source.setLastIngestedAt(lastIngestedAt);
		}
	}

	@Override
	public void restoreIngestedAt(Long linkId, Date ingestedAt) {
		EntitySourceLinkImpl link = linkId == null ? null
				: entityManager.find(EntitySourceLinkImpl.class, linkId);
		if (link != null && ingestedAt != null) {
			link.setIngestedAt(ingestedAt);
		}
	}

	@Override
	public boolean unlink(Long sourceId, SourceLinkRelation relation, String targetType,
			Long targetId, String fragment) {
		EntitySourceLink link = find(sourceId, relation, targetType, targetId,
				ProvenanceStore.normalizeFragment(fragment));
		if (link == null) {
			return false;
		}
		entityManager.remove(link);
		return true;
	}

	@Override
	public int deleteForTarget(String targetType, Long targetId) {
		if (targetType == null || targetId == null) {
			return 0;
		}
		return entityManager.createQuery("delete from EntitySourceLinkImpl l "
				+ "where l.targetType = :targetType and l.targetId = :targetId")
				.setParameter("targetType", targetType).setParameter("targetId", targetId)
				.executeUpdate();
	}

	@Override
	public int deleteForProject(Long projectId) {
		if (projectId == null) {
			return 0;
		}
		int links = entityManager.createQuery(
				"delete from EntitySourceLinkImpl l where l.projectId = :projectId")
				.setParameter("projectId", projectId).executeUpdate();
		int sources = entityManager.createQuery(
				"delete from ExternalSourceImpl s where s.projectId = :projectId")
				.setParameter("projectId", projectId).executeUpdate();
		return links + sources;
	}

	private EntitySourceLink find(Long sourceId, SourceLinkRelation relation, String targetType,
			Long targetId, String fragment) {
		if (sourceId == null || relation == null || targetType == null || targetId == null) {
			return null;
		}
		String key = ProvenanceStore.fragmentKey(fragment);
		return entityManager.createQuery("select l from EntitySourceLinkImpl l "
				+ "where l.source.id = :sourceId and l.relation = :relation "
				+ "and l.targetType = :targetType and l.targetId = :targetId "
				+ "and l.fragmentKey = :fragmentKey", EntitySourceLinkImpl.class)
				.setParameter("sourceId", sourceId).setParameter("relation", relation)
				.setParameter("targetType", targetType).setParameter("targetId", targetId)
				.setParameter("fragmentKey", key).getResultList().stream()
				.filter(l -> key.equals(l.getFragmentKey())).findFirst().orElse(null);
	}

	private static String truncate(String text, int max) {
		return (text == null || text.length() <= max) ? text : text.substring(0, max);
	}
}
