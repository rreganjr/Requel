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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import com.rreganjr.requel.gateway.ProjectContentTooLargeException;
import com.rreganjr.requel.service.api.dto.ProjectContentDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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

    // ---- #274: the project content read --------------------------------------------------------

    @Test
    void getProjectContentPassesTheAnnotationModeWhenGiven() {
        Fixture f = newFixture();
        f.server().expect(requestTo("/api/gateway/query/projects/Demo/content?annotations=open"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"annotations\":\"OPEN\",\"goals\":[{\"id\":7,"
                        + "\"name\":\"G\",\"text\":\"Goal text\"}]}", MediaType.APPLICATION_JSON));

        ProjectContentDto content = f.gateway().getProjectContent("Demo", "open");

        assertThat(content.annotations()).isEqualTo("OPEN");
        assertThat(content.goals()).singleElement()
                .satisfies(g -> assertThat(g.text()).isEqualTo("Goal text"));
        f.server().verify();
    }

    @Test
    void getProjectContentLeavesTheModeOffWhenAbsent() {
        Fixture f = newFixture();
        f.server().expect(requestTo("/api/gateway/query/projects/Demo/content"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"annotations\":\"ALL\"}", MediaType.APPLICATION_JSON));

        assertThat(f.gateway().getProjectContent("Demo", null).annotations()).isEqualTo("ALL");
        f.server().verify();
    }

    @Test
    void aContentTooLargeResponseComesBackAsTheTypedExceptionWithTheServersMessage() {
        Fixture f = newFixture();
        String message = "Project 'Demo' content is 900 characters; the cap is 100";
        f.server().expect(requestTo("/api/gateway/query/projects/Demo/content"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"CONTENT_TOO_LARGE\",\"message\":\"" + message
                                + "\",\"timestamp\":\"2026-09-29T00:00:00Z\"}"));

        assertThatThrownBy(() -> f.gateway().getProjectContent("Demo", null))
                .isInstanceOf(ProjectContentTooLargeException.class)
                .hasMessage(message);
    }

    @Test
    void anyOther422StaysAnHttpError() {
        Fixture f = newFixture();
        f.server().expect(requestTo("/api/gateway/query/projects/Demo/content"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"OTHER\",\"message\":\"no\"}"));

        assertThatThrownBy(() -> f.gateway().getProjectContent("Demo", null))
                .isInstanceOf(RestClientResponseException.class);
    }
}
