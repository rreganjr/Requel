/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.IgnoredFinding;
import com.rreganjr.requel.project.IgnoredFindingStore;
import com.rreganjr.requel.project.impl.IgnoredFindingImpl;

/**
 * Issue #320: the JPA {@link IgnoredFindingStore}.
 */
@Repository("ignoredFindingStore")
@Scope("singleton")
@Transactional(propagation = Propagation.REQUIRED)
public class JpaIgnoredFindingStore implements IgnoredFindingStore {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Set<String> ignoredKeys(Long projectId) {
		if (projectId == null) {
			return Collections.emptySet();
		}
		return new HashSet<String>(entityManager.createQuery(
				"select f.keyLower from IgnoredFindingImpl f where f.projectId = :projectId",
				String.class).setParameter("projectId", projectId).getResultList());
	}

	@Override
	public IgnoredFinding record(Spec spec, User createdBy) {
		String keyLower = spec.idempotencyKey().toLowerCase(Locale.ROOT);
		List<IgnoredFindingImpl> existing = entityManager.createQuery(
				"select f from IgnoredFindingImpl f where f.keyLower = :key", IgnoredFindingImpl.class)
				.setParameter("key", keyLower).getResultList();
		if (!existing.isEmpty()) {
			return existing.get(0);
		}
		IgnoredFindingImpl ignored = new IgnoredFindingImpl(spec.projectId(), spec.targetType(),
				spec.targetId(), spec.assistantId(), spec.findingType(), spec.propertyName(),
				spec.keySuffix(), truncate(spec.subject(), 500), spec.annotationId(), createdBy);
		entityManager.persist(ignored);
		return ignored;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<IgnoredFinding> list(Long projectId) {
		if (projectId == null) {
			return Collections.emptyList();
		}
		return List.copyOf(entityManager.createQuery(
				"select f from IgnoredFindingImpl f where f.projectId = :projectId "
						+ "order by f.targetType, f.targetId, f.subject, f.id",
				IgnoredFindingImpl.class).setParameter("projectId", projectId).getResultList());
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Optional<IgnoredFinding> find(Long projectId, Long id) {
		if (projectId == null || id == null) {
			return Optional.empty();
		}
		IgnoredFindingImpl found = entityManager.find(IgnoredFindingImpl.class, id);
		return (found != null && projectId.equals(found.getProjectId()))
				? Optional.<IgnoredFinding>of(found) : Optional.empty();
	}

	@Override
	public boolean delete(Long projectId, Long id) {
		if (projectId == null || id == null) {
			return false;
		}
		return entityManager.createQuery(
				"delete from IgnoredFindingImpl f where f.id = :id and f.projectId = :projectId")
				.setParameter("id", id).setParameter("projectId", projectId).executeUpdate() > 0;
	}

	@Override
	public int deleteForTarget(String targetType, Long targetId) {
		if (targetType == null || targetId == null) {
			return 0;
		}
		return entityManager.createQuery("delete from IgnoredFindingImpl f "
				+ "where f.targetType = :targetType and f.targetId = :targetId")
				.setParameter("targetType", targetType).setParameter("targetId", targetId)
				.executeUpdate();
	}

	@Override
	public int deleteForProject(Long projectId) {
		if (projectId == null) {
			return 0;
		}
		return entityManager.createQuery(
				"delete from IgnoredFindingImpl f where f.projectId = :projectId")
				.setParameter("projectId", projectId).executeUpdate();
	}

	private static String truncate(String text, int max) {
		return (text == null || text.length() <= max) ? text : text.substring(0, max);
	}
}
