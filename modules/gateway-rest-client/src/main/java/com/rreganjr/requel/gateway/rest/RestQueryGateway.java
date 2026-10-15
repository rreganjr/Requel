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
package com.rreganjr.requel.gateway.rest;

import com.rreganjr.requel.gateway.ProjectContentTooLargeException;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.GlossaryTermDto;
import com.rreganjr.requel.service.api.dto.OpenIssueDto;
import com.rreganjr.requel.service.api.dto.ProjectContentDto;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * REST-backed {@link QueryGateway}: the read side of {@code gateway-rest-client}, calling the
 * {@code /api/gateway/query/**} facade (which delegates to the in-process query gateway, so read
 * authorization is enforced server-side exactly as in the UI). Front-ends such as {@code requel-cli}
 * consume the same {@link QueryGateway} contract regardless of in-process vs. REST.
 *
 * <p>The interface declares no checked exceptions; on an HTTP error the underlying
 * {@link RestClient} throws {@link org.springframework.web.client.RestClientResponseException}
 * (unchecked), which a front-end can map to an exit code / message.
 */
public class RestQueryGateway implements QueryGateway {

    private static final String BASE = "/api/gateway/query";

    private final RestClient http;

    public RestQueryGateway(String baseUrl, BearerTokenSource tokenSource) {
        this(RestClients.bearer(baseUrl, tokenSource));
    }

    /** For tests: inject a preconfigured (e.g. MockRestServiceServer-bound) client. */
    RestQueryGateway(RestClient http) {
        this.http = http;
    }

    @Override
    public List<ProjectDto> listProjects() {
        return http.get().uri(BASE + "/projects")
                .retrieve().body(new ParameterizedTypeReference<>() { });
    }

    @Override
    public ProjectDto getProject(String projectName) {
        return http.get().uri(BASE + "/projects/{name}", projectName)
                .retrieve().body(ProjectDto.class);
    }

    @Override
    public List<ProjectTreeNodeDto> getProjectTree(String projectName) {
        return http.get().uri(BASE + "/projects/{name}/tree", projectName)
                .retrieve().body(new ParameterizedTypeReference<>() { });
    }

    @Override
    public List<GlossaryTermDto> getGlossaryTerms(String projectName) {
        return http.get().uri(BASE + "/projects/{name}/glossary", projectName)
                .retrieve().body(new ParameterizedTypeReference<>() { });
    }

    @Override
    public List<OpenIssueDto> getOpenIssues(String projectName) {
        return http.get().uri(BASE + "/projects/{name}/open-issues", projectName)
                .retrieve().body(new ParameterizedTypeReference<>() { });
    }

    @Override
    public AnnotationsDto getAnnotations(String projectName, String entityType, long entityId) {
        return http.get()
                .uri(BASE + "/projects/{name}/annotations?entityType={type}&entityId={id}",
                        projectName, entityType, entityId)
                .retrieve().body(AnnotationsDto.class);
    }

    @Override
    public com.rreganjr.requel.service.api.dto.ExternalSourceDto getSource(String projectName,
            String system, String externalId) {
        return http.get()
                .uri(BASE + "/projects/{name}/source?system={system}&externalId={externalId}",
                        projectName, system, externalId)
                .retrieve().body(com.rreganjr.requel.service.api.dto.ExternalSourceDto.class);
    }

    @Override
    public com.rreganjr.requel.service.api.dto.SourceEntitiesDto findEntitiesBySource(
            String projectName, String system, String externalId, String fragment) {
        return http.get()
                .uri(fragment == null
                        ? BASE + "/projects/{name}/source/entities?system={system}"
                                + "&externalId={externalId}"
                        : BASE + "/projects/{name}/source/entities?system={system}"
                                + "&externalId={externalId}&fragment={fragment}",
                        projectName, system, externalId, fragment)
                .retrieve().body(com.rreganjr.requel.service.api.dto.SourceEntitiesDto.class);
    }

    @Override
    public com.rreganjr.requel.service.api.dto.ProjectSourcesDto listSources(String projectName) {
        return http.get().uri(BASE + "/projects/{name}/sources", projectName)
                .retrieve().body(com.rreganjr.requel.service.api.dto.ProjectSourcesDto.class);
    }

    @Override
    public com.rreganjr.requel.service.api.dto.SourceComparisonDto compareSources(
            String projectName, String system, String externalId, String otherSystem,
            String otherExternalId) {
        return http.get()
                .uri(BASE + "/projects/{name}/sources/compare?system={system}"
                        + "&externalId={externalId}&otherSystem={otherSystem}"
                        + "&otherExternalId={otherExternalId}",
                        projectName, system, externalId, otherSystem, otherExternalId)
                .retrieve().body(com.rreganjr.requel.service.api.dto.SourceComparisonDto.class);
    }

    @Override
    public List<com.rreganjr.requel.service.api.dto.EntitySourceLinkDto> getEntitySources(
            String projectName, String entityType, long entityId) {
        return http.get()
                .uri(BASE + "/projects/{name}/entity/sources?entityType={type}&entityId={id}",
                        projectName, entityType, entityId)
                .retrieve().body(new org.springframework.core.ParameterizedTypeReference<
                        List<com.rreganjr.requel.service.api.dto.EntitySourceLinkDto>>() {
                });
    }

    @Override
    public Object getEntity(String projectName, String entityType, long entityId) {
        return http.get()
                .uri(BASE + "/projects/{name}/entity?entityType={type}&entityId={id}",
                        projectName, entityType, entityId)
                .retrieve().body(Object.class);
    }

    @Override
    public Map<String, List<EntityReferenceDto>> getEntityNeighbors(String projectName,
            String entityType, long entityId) {
        return http.get()
                .uri(BASE + "/projects/{name}/entity/neighbors?entityType={type}&entityId={id}",
                        projectName, entityType, entityId)
                .retrieve().body(new ParameterizedTypeReference<>() { });
    }

    @Override
    public List<EntityReferenceDto> searchProjectEntities(String projectName, String query) {
        return http.get()
                .uri(BASE + "/projects/{name}/search?q={q}", projectName, query)
                .retrieve().body(new ParameterizedTypeReference<>() { });
    }

    @Override
    public Map<String, Object> getProjectContext(String projectName) {
        return http.get().uri(BASE + "/projects/{name}/context", projectName)
                .retrieve().body(new ParameterizedTypeReference<>() { });
    }

    /**
     * Issue #274. A 422 {@code CONTENT_TOO_LARGE} comes back as
     * {@link ProjectContentTooLargeException} with the server's message, which names the overflow.
     */
    @Override
    public ProjectContentDto getProjectContent(String projectName, String annotations) {
        try {
            return http.get()
                    .uri(annotations == null
                                    ? BASE + "/projects/{name}/content"
                                    : BASE + "/projects/{name}/content?annotations={annotations}",
                            projectName, annotations)
                    .retrieve().body(ProjectContentDto.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 422) {
                String message = contentTooLargeMessage(e);
                if (message != null) {
                    throw new ProjectContentTooLargeException(message);
                }
            }
            throw e;
        }
    }

    /** The server's message when the body is a {@code CONTENT_TOO_LARGE} error, else null. */
    private static String contentTooLargeMessage(RestClientResponseException e) {
        try {
            Map<?, ?> body = e.getResponseBodyAs(Map.class);
            if (body != null && ProjectContentTooLargeException.CODE.equals(body.get("error"))
                    && body.get("message") instanceof String message) {
                return message;
            }
        } catch (RuntimeException unreadable) {
            // not the expected shape: fall through to the plain HTTP error
        }
        return null;
    }
}
