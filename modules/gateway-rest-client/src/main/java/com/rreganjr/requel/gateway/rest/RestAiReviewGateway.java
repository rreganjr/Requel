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
package com.rreganjr.requel.gateway.rest;

import com.rreganjr.requel.gateway.AiReviewGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.service.api.dto.AiReviewDto;
import com.rreganjr.requel.service.api.dto.AiReviewRequestDto;
import java.util.Optional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Issue #387: REST-backed {@link AiReviewGateway} for {@code requel review}, calling
 * {@code /api/gateway/ai/reviews/{projectName}}. Failures come back as {@link GatewayException}s
 * with the server's kind, as {@link RestCommandGateway}'s do; the request is refused (NOT_ALLOWED)
 * when the server has writes disabled.
 */
public class RestAiReviewGateway implements AiReviewGateway {

    private static final String BASE = "/api/gateway/ai/reviews/{projectName}";

    private final RestClient http;

    public RestAiReviewGateway(String baseUrl, BearerTokenSource tokenSource) {
        this(RestClients.bearer(baseUrl, tokenSource));
    }

    /** For tests: inject a preconfigured (e.g. MockRestServiceServer-bound) client. */
    RestAiReviewGateway(RestClient http) {
        this.http = http;
    }

    @Override
    public AiReviewRequestDto requestReview(String projectName, String entityType, long entityId)
            throws GatewayException {
        try {
            return http.post()
                    .uri(BASE + "?entityType={type}&entityId={id}", projectName, entityType,
                            entityId)
                    .header("X-Requel-Client", RestCommandGateway.DEFAULT_CLIENT_ID)
                    .retrieve().body(AiReviewRequestDto.class);
        } catch (RestClientResponseException e) {
            throw RestCommandGateway.toGatewayException(e);
        } catch (RestClientException e) {
            throw unreachable(e);
        }
    }

    @Override
    public Optional<AiReviewDto> latestReview(String projectName, String entityType,
            long entityId, String taskType) throws GatewayException {
        try {
            String uri = BASE + "?entityType={type}&entityId={id}"
                    + (taskType == null ? "" : "&taskType={taskType}");
            // 204 (never reviewed) has no body, so the read comes back null.
            return Optional.ofNullable(http.get()
                    .uri(uri, projectName, entityType, entityId, taskType)
                    .retrieve().body(AiReviewDto.class));
        } catch (RestClientResponseException e) {
            throw RestCommandGateway.toGatewayException(e);
        } catch (RestClientException e) {
            throw unreachable(e);
        }
    }

    private static GatewayException unreachable(RestClientException e) {
        return new GatewayException(GatewayException.Kind.EXECUTION_ERROR,
                "Failed to reach Requel: " + e.getMessage(), e);
    }
}
