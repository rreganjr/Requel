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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.service.api.dto.AiReviewDto;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Issue #387: the CLI's AI review client against {@code /api/gateway/ai/reviews}. */
class RestAiReviewGatewayTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final RestAiReviewGateway gateway = new RestAiReviewGateway(builder.build());

    @Test
    void aRequestPostsTheEntityAndReturnsTheRuns() throws Exception {
        server.expect(requestTo("/api/gateway/ai/reviews/Demo?entityType=Goal&entityId=7"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Requel-Client", "requel-cli"))
                .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"runId\":\"r1\",\"policyRunId\":\"r2\"}"));

        assertThat(gateway.requestReview("Demo", "Goal", 7L).policyRunId()).isEqualTo("r2");
        server.verify();
    }

    @Test
    void anUnavailableReviewComesBackAsAConflictWithTheReason() {
        server.expect(requestTo("/api/gateway/ai/reviews/Demo?entityType=Goal&entityId=7"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"kind\":\"CONFLICT\",\"message\":\"AI review is off on this server.\"}"));

        assertThatThrownBy(() -> gateway.requestReview("Demo", "Goal", 7L))
                .isInstanceOfSatisfying(GatewayException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(GatewayException.Kind.CONFLICT);
                    assertThat(e.getMessage()).isEqualTo("AI review is off on this server.");
                });
    }

    @Test
    void theLatestRunIsReadWithItsTimesAndTheTaskTypeOnlyWhenGiven() throws Exception {
        server.expect(requestTo("/api/gateway/ai/reviews/Demo?entityType=Goal&entityId=7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"runId\":\"r1\",\"status\":\"SUCCEEDED\","
                        + "\"createdAt\":\"2026-10-30T12:00:00Z\",\"findings\":[{\"kind\":\"ISSUE\","
                        + "\"severity\":\"HIGH\",\"text\":\"Vague.\"}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "/api/gateway/ai/reviews/Demo?entityType=Goal&entityId=7&taskType=POLICY_REVIEW"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        AiReviewDto run = gateway.latestReview("Demo", "Goal", 7L, null).orElseThrow();
        assertThat(run.createdAt()).isEqualTo(Instant.parse("2026-10-30T12:00:00Z"));
        assertThat(run.findings()).singleElement()
                .satisfies(f -> assertThat(f.text()).isEqualTo("Vague."));
        assertThat(gateway.latestReview("Demo", "Goal", 7L, "POLICY_REVIEW")).isEmpty();
        server.verify();
    }
}
