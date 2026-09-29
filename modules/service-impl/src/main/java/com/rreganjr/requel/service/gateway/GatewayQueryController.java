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
package com.rreganjr.requel.service.gateway;

import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.GlossaryTermDto;
import com.rreganjr.requel.service.api.dto.OpenIssueDto;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST facade over the {@link QueryGateway} read contract, for out-of-process front-ends (the
 * REST-backed gateway client used by {@code requel-cli}). Exposes the whole {@link QueryGateway}
 * surface 1:1 under {@code /api/gateway/query/**} so the client maps straight onto it and stays
 * decoupled from the UI's query controllers.
 *
 * <p>It delegates to the injected in-process {@link QueryGateway} bean, which itself forwards to the
 * same query controllers the UI uses — so read authorization (project membership, per-entity gating)
 * is enforced exactly as elsewhere. Sits under {@code /api/**}, so the standard JWT/PAT/OAuth
 * security chain authenticates it.
 */
@RestController
@RequestMapping("/api/gateway/query")
public class GatewayQueryController {

    private final QueryGateway queryGateway;

    public GatewayQueryController(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping("/projects")
    public List<ProjectDto> listProjects() {
        return queryGateway.listProjects();
    }

    @GetMapping("/projects/{name}")
    public ProjectDto getProject(@PathVariable String name) {
        return queryGateway.getProject(name);
    }

    @GetMapping("/projects/{name}/tree")
    public List<ProjectTreeNodeDto> getProjectTree(@PathVariable String name) {
        return queryGateway.getProjectTree(name);
    }

    @GetMapping("/projects/{name}/glossary")
    public List<GlossaryTermDto> getGlossaryTerms(@PathVariable String name) {
        return queryGateway.getGlossaryTerms(name);
    }

    @GetMapping("/projects/{name}/open-issues")
    public List<OpenIssueDto> getOpenIssues(@PathVariable String name) {
        return queryGateway.getOpenIssues(name);
    }

    @GetMapping("/projects/{name}/annotations")
    public AnnotationsDto getAnnotations(@PathVariable String name,
            @RequestParam String entityType, @RequestParam long entityId) {
        return queryGateway.getAnnotations(name, entityType, entityId);
    }

    @GetMapping("/projects/{name}/entity")
    public Object getEntity(@PathVariable String name,
            @RequestParam String entityType, @RequestParam long entityId) {
        return queryGateway.getEntity(name, entityType, entityId);
    }

    @GetMapping("/projects/{name}/entity/neighbors")
    public Map<String, List<EntityReferenceDto>> getEntityNeighbors(@PathVariable String name,
            @RequestParam String entityType, @RequestParam long entityId) {
        return queryGateway.getEntityNeighbors(name, entityType, entityId);
    }

    @GetMapping("/projects/{name}/search")
    public List<EntityReferenceDto> searchProjectEntities(@PathVariable String name,
            @RequestParam("q") String query) {
        return queryGateway.searchProjectEntities(name, query);
    }

    /** Issue #272: a source by system and external id; an empty body when there is none. */
    @GetMapping("/projects/{name}/source")
    public com.rreganjr.requel.service.api.dto.ExternalSourceDto getSource(
            @PathVariable String name, @RequestParam String system,
            @RequestParam String externalId) {
        return queryGateway.getSource(name, system, externalId);
    }

    /** Issue #272: the entities a source produced; an empty body when there is no such source. */
    @GetMapping("/projects/{name}/source/entities")
    public com.rreganjr.requel.service.api.dto.SourceEntitiesDto findEntitiesBySource(
            @PathVariable String name, @RequestParam String system,
            @RequestParam String externalId,
            @RequestParam(required = false) String fragment) {
        return queryGateway.findEntitiesBySource(name, system, externalId, fragment);
    }

    /** Issue #273: every source and reference of the project, with precedence. */
    @GetMapping("/projects/{name}/sources")
    public com.rreganjr.requel.service.api.dto.ProjectSourcesDto listSources(
            @PathVariable String name) {
        return queryGateway.listSources(name);
    }

    /** Issue #273: which of two sources wins. */
    @GetMapping("/projects/{name}/sources/compare")
    public com.rreganjr.requel.service.api.dto.SourceComparisonDto compareSources(
            @PathVariable String name, @RequestParam String system,
            @RequestParam String externalId, @RequestParam String otherSystem,
            @RequestParam String otherExternalId) {
        return queryGateway.compareSources(name, system, externalId, otherSystem,
                otherExternalId);
    }

    /** Issue #272: which sources an entity came from. */
    @GetMapping("/projects/{name}/entity/sources")
    public List<com.rreganjr.requel.service.api.dto.EntitySourceLinkDto> getEntitySources(
            @PathVariable String name, @RequestParam String entityType,
            @RequestParam long entityId) {
        return queryGateway.getEntitySources(name, entityType, entityId);
    }

    @GetMapping("/projects/{name}/context")
    public Map<String, Object> getProjectContext(@PathVariable String name) {
        return queryGateway.getProjectContext(name);
    }
}
