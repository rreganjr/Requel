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

    @GetMapping("/projects/{name}/context")
    public Map<String, Object> getProjectContext(@PathVariable String name) {
        return queryGateway.getProjectContext(name);
    }
}
