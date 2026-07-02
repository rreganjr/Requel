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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestCommandGatewayTest {

    private record Fixture(RestCommandGateway gateway, MockRestServiceServer server) {
    }

    private static Fixture newFixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new RestCommandGateway(builder.build()), server);
    }

    @Test
    void dispatchesToGatewayEndpointAndReturnsResult() throws Exception {
        Fixture f = newFixture();
        f.server().expect(requestTo("/api/gateway/commands/EditGoal"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Requel-Client", "requel-cli"))
                .andExpect(jsonPath("$.name").value("My Goal"))
                .andRespond(withSuccess("{\"id\":7,\"name\":\"My Goal\"}", MediaType.APPLICATION_JSON));

        GatewayResult result = f.gateway().execute(new GatewayRequest("EditGoal", Map.of("name", "My Goal")));

        assertThat(result.commandType()).isEqualTo("EditGoal");
        assertThat(result.result()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) result.result()).get("id")).isEqualTo(7);
        f.server().verify();
    }

    @Test
    void notAllowedKindComesFromErrorBody() {
        Fixture f = newFixture();
        f.server().expect(requestTo("/api/gateway/commands/DeleteUser"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"kind\":\"NOT_ALLOWED\",\"message\":\"command 'DeleteUser' is denylisted\"}"));

        assertThatThrownBy(() -> f.gateway().execute(new GatewayRequest("DeleteUser", Map.of())))
                .isInstanceOf(GatewayException.class)
                .satisfies(e -> {
                    assertThat(((GatewayException) e).getKind()).isEqualTo(GatewayException.Kind.NOT_ALLOWED);
                    assertThat(e.getMessage()).contains("denylisted");
                });
    }

    @Test
    void unauthorizedAndNotAllowedShareStatusButAreDistinguishedByBodyKind() {
        Fixture f = newFixture();
        f.server().expect(requestTo("/api/gateway/commands/EditGoal"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"kind\":\"UNAUTHORIZED\",\"message\":\"no permission\"}"));

        assertThatThrownBy(() -> f.gateway().execute(new GatewayRequest("EditGoal", Map.of())))
                .isInstanceOf(GatewayException.class)
                .satisfies(e -> assertThat(((GatewayException) e).getKind())
                        .isEqualTo(GatewayException.Kind.UNAUTHORIZED));
    }

    @Test
    void notFoundKindComesFromErrorBody() {
        Fixture f = newFixture();
        f.server().expect(requestTo("/api/gateway/commands/NoSuchCommand"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"kind\":\"NOT_FOUND\",\"message\":\"Unknown command type: NoSuchCommand\"}"));

        assertThatThrownBy(() -> f.gateway().execute(new GatewayRequest("NoSuchCommand", Map.of())))
                .isInstanceOf(GatewayException.class)
                .satisfies(e -> assertThat(((GatewayException) e).getKind())
                        .isEqualTo(GatewayException.Kind.NOT_FOUND));
    }

    @Test
    void emptyBodyFallsBackToStatusMapping() {
        // Simulates a 401 from the security layer (before the request reaches the gateway controller),
        // which has no GatewayErrorBody — the client falls back to mapping the status.
        Fixture f = newFixture();
        f.server().expect(requestTo("/api/gateway/commands/EditGoal"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> f.gateway().execute(new GatewayRequest("EditGoal", Map.of())))
                .isInstanceOf(GatewayException.class)
                .satisfies(e -> assertThat(((GatewayException) e).getKind())
                        .isEqualTo(GatewayException.Kind.UNAUTHORIZED));
    }
}
