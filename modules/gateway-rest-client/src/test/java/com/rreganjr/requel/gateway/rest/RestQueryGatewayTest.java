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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestQueryGatewayTest {

    private record Fixture(RestQueryGateway gateway, MockRestServiceServer server) {
    }

    private static Fixture newFixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new RestQueryGateway(builder.build()), server);
    }

    @Test
    void listProjectsCallsGatewayEndpoint() {
        Fixture f = newFixture();
        f.server().expect(requestTo("/api/gateway/query/projects"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<?> projects = f.gateway().listProjects();

        assertThat(projects).isEmpty();
        f.server().verify();
    }

    @Test
    void getEntityPassesTypeAndIdAsQueryParams() {
        Fixture f = newFixture();
        f.server().expect(requestTo("/api/gateway/query/projects/Demo/entity?entityType=Goal&entityId=7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":7,\"name\":\"My Goal\"}", MediaType.APPLICATION_JSON));

        Object entity = f.gateway().getEntity("Demo", "Goal", 7L);

        assertThat(entity).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) entity).get("name")).isEqualTo("My Goal");
        f.server().verify();
    }

    @Test
    void getProjectContextReturnsKeyedBundle() {
        Fixture f = newFixture();
        f.server().expect(requestTo("/api/gateway/query/projects/Demo/context"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"project\":{\"name\":\"Demo\"},\"openIssues\":[]}",
                        MediaType.APPLICATION_JSON));

        Map<String, Object> context = f.gateway().getProjectContext("Demo");

        assertThat(context).containsKeys("project", "openIssues");
        f.server().verify();
    }
}
