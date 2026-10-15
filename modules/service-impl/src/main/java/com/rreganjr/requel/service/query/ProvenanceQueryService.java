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
package com.rreganjr.requel.service.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.project.EntitySourceLink;
import com.rreganjr.requel.project.ExternalSource;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProvenanceStore;
import com.rreganjr.requel.project.SourceAuthority;
import com.rreganjr.requel.project.SourceAuthorityEdge;
import com.rreganjr.requel.project.SourceLinkRelation;
import com.rreganjr.requel.project.SourceLinks;
import com.rreganjr.requel.project.impl.ProvenanceEntityTypes;
import com.rreganjr.requel.service.api.dto.EntitySourceLinkDto;
import com.rreganjr.requel.service.api.dto.ExternalSourceDto;
import com.rreganjr.requel.service.api.dto.ProjectSourceDto;
import com.rreganjr.requel.service.api.dto.ProjectSourcesDto;
import com.rreganjr.requel.service.api.dto.SourceAuthorityDto;
import com.rreganjr.requel.service.api.dto.SourceComparisonDto;
import com.rreganjr.requel.service.api.dto.SourceEntitiesDto;
import com.rreganjr.requel.service.api.dto.SourceRefDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;

/**
 * Issue #272: the provenance reads — the only reads that return a source or its locator. The
 * general reads (getEntity, getProjectContext, getAnnotations, the assistant context packs) never
 * reach the provenance tables, which is how "no provenance URL is ever passed to a model" holds
 * by construction. Each read needs read access to the project, as every project read does.
 * <p>
 * A link whose entity no longer exists (a delete path that bypassed the store) is left out.
 * <p>
 * Issue #273: the same holds for references — every source is one — and for citations and the
 * precedence between sources, which only these reads (and the report generators) return.
 */
@Service
@Transactional(readOnly = true)
public class ProvenanceQueryService {

	private final ProjectRepository projectRepository;
	private final ProvenanceStore provenanceStore;
	private final CurrentUserResolver currentUserResolver;

	public ProvenanceQueryService(ProjectRepository projectRepository,
			ProvenanceStore provenanceStore, CurrentUserResolver currentUserResolver) {
		this.projectRepository = projectRepository;
		this.provenanceStore = provenanceStore;
		this.currentUserResolver = currentUserResolver;
	}

	/**
	 * @return the source, or empty when the project has none by that system and external id.
	 */
	public Optional<ExternalSourceDto> getSource(String projectName, String system,
			String externalId) {
		Project project = readableProject(projectName);
		return provenanceStore.findSource(project.getId(), system, externalId)
				.map(ProvenanceQueryService::toSourceDto);
	}

	/**
	 * @param fragment null for every fragment of the source; otherwise that fragment only ("" is
	 *                 not accepted: use null)
	 * @return the source and the entities it produced, or empty when there is no such source.
	 */
	public Optional<SourceEntitiesDto> findEntitiesBySource(String projectName, String system,
			String externalId, String fragment) {
		Project project = readableProject(projectName);
		Optional<ExternalSource> source = provenanceStore.findSource(project.getId(), system,
				externalId);
		if (source.isEmpty()) {
			return Optional.empty();
		}
		String wanted = ProvenanceStore.normalizeFragment(fragment);
		List<EntitySourceLinkDto> links = new ArrayList<>();
		for (EntitySourceLink link : provenanceStore.linksForSource(source.get().getId())) {
			if (wanted != null && !wanted.equals(link.getFragment())) {
				continue;
			}
			toLinkDto(project, link).ifPresent(links::add);
		}
		return Optional.of(new SourceEntitiesDto(toSourceDto(source.get()), links));
	}

	/**
	 * @return the entity's links, ordered by source then fragment; empty for an entity with none.
	 * @throws IllegalArgumentException for an entity type that cannot carry a source, or an
	 *                                  entity that is not in the project
	 */
	public List<EntitySourceLinkDto> getEntitySources(String projectName, String entityType,
			long entityId) {
		Project project = readableProject(projectName);
		Class<? extends ProjectOrDomainEntity> type = ProvenanceEntityTypes.require(entityType);
		ProvenanceEntityTypes.load(projectRepository, project, type, entityId)
				.orElseThrow(() -> new IllegalArgumentException(
						entityType + " " + entityId + " not found in project " + projectName));
		List<EntitySourceLinkDto> links = new ArrayList<>();
		for (EntitySourceLink link : provenanceStore.linksForTarget(entityType, entityId)) {
			toLinkDto(project, link).ifPresent(links::add);
		}
		return links;
	}

	/**
	 * Issue #273: every source of the project — its references, whether or not anything was
	 * built from them — with link counts and the precedence edges between them.
	 */
	public ProjectSourcesDto listSources(String projectName) {
		Project project = readableProject(projectName);
		List<ExternalSource> sources = provenanceStore.listSources(project.getId());
		List<SourceAuthorityEdge> edges = provenanceStore.authorityEdges(project.getId());
		Map<Long, ExternalSource> byId = new HashMap<>();
		sources.forEach(source -> byId.put(source.getId(), source));
		List<SourceAuthority.Edge> ids = SourceAuthority.edgesOf(edges);
		List<ProjectSourceDto> dtos = new ArrayList<>();
		for (ExternalSource source : sources) {
			dtos.add(new ProjectSourceDto(toSourceDto(source),
					provenanceStore.countLinks(source.getId(), SourceLinkRelation.DERIVED_FROM),
					provenanceStore.countLinks(source.getId(), SourceLinkRelation.CITES),
					refs(SourceAuthority.superiorsOf(ids, source.getId()), byId),
					refs(SourceAuthority.subordinatesOf(ids, source.getId()), byId)));
		}
		List<SourceAuthorityDto> authority = edges.stream()
				.map(edge -> new SourceAuthorityDto(toRef(edge.getSubordinate()),
						toRef(edge.getSuperior()), edge.getNote()))
				.toList();
		return new ProjectSourcesDto(dtos, authority);
	}

	/**
	 * Issue #273: which of two sources wins where they disagree, following "defers to" edges
	 * transitively.
	 *
	 * @throws IllegalArgumentException when either source is not recorded in the project
	 */
	public SourceComparisonDto compareSources(String projectName, String system,
			String externalId, String otherSystem, String otherExternalId) {
		Project project = readableProject(projectName);
		ExternalSource a = requireSource(project, system, externalId);
		ExternalSource b = requireSource(project, otherSystem, otherExternalId);
		Map<Long, ExternalSource> byId = new HashMap<>();
		provenanceStore.listSources(project.getId()).forEach(s -> byId.put(s.getId(), s));
		SourceAuthority.Resolution resolution = SourceAuthority.resolve(
				SourceAuthority.edgesOf(provenanceStore.authorityEdges(project.getId())),
				a.getId(), b.getId());
		return new SourceComparisonDto(resolution.winner().name(), toRef(a), toRef(b),
				refs(resolution.chain(), byId));
	}

	private ExternalSource requireSource(Project project, String system, String externalId) {
		return provenanceStore.findSource(project.getId(), system, externalId)
				.orElseThrow(() -> new IllegalArgumentException("project '" + project.getName()
						+ "' has no source " + system + " " + externalId));
	}

	private static List<SourceRefDto> refs(List<Long> ids, Map<Long, ExternalSource> byId) {
		return ids.stream().map(byId::get).filter(java.util.Objects::nonNull)
				.map(ProvenanceQueryService::toRef).toList();
	}

	public static SourceRefDto toRef(ExternalSource source) {
		return source == null ? null
				: new SourceRefDto(source.getSystem(), source.getExternalId(), source.getTitle());
	}

	private Project readableProject(String projectName) {
		Project project = projectRepository.findProjectByName(projectName);
		if (!ProjectReadAccess.canRead(project, currentUserResolver.resolve())) {
			throw new AuthorizationException("You do not have access to this project.");
		}
		return project;
	}

	private Optional<EntitySourceLinkDto> toLinkDto(Project project, EntitySourceLink link) {
		Class<? extends ProjectOrDomainEntity> type = ProvenanceEntityTypes.BY_NAME
				.get(link.getTargetType());
		Optional<ProjectOrDomainEntity> entity = ProvenanceEntityTypes.load(projectRepository,
				project, type, link.getTargetId());
		if (entity.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new EntitySourceLinkDto(link.getId(), toSourceDto(link.getSource()),
				link.getRelation().name(), link.getTargetType(), link.getTargetId(),
				entity.get().getName(), link.getFragment(), link.getIngestedAt(),
				link.isNotInLatestSource(), editedSinceIngest(link, entity.get())));
	}

	/**
	 * A citation (#273) records no entity state, so "edited since ingest" does not apply to it;
	 * without this it would read as edited, the way a #71 link with no fingerprint does.
	 */
	static boolean editedSinceIngest(EntitySourceLink link, ProjectOrDomainEntity entity) {
		return link.getRelation() != SourceLinkRelation.CITES
				&& SourceLinks.isEditedSinceIngest(link, entity);
	}

	public static ExternalSourceDto toSourceDto(ExternalSource source) {
		if (source == null) {
			return null;
		}
		return new ExternalSourceDto(source.getId(), source.getSystem(), source.getExternalId(),
				source.getLocatorType() == null ? null : source.getLocatorType().name(),
				source.getLocator(), source.getTitle(), source.getContentHash(),
				source.getLastIngestedAt(), source.getKind(), source.getNote());
	}
}
