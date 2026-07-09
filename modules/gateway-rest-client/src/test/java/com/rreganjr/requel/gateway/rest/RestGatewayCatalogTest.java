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
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestGatewayCatalogTest {

    private record Fixture(RestGatewayCatalog catalog, MockRestServiceServer server) {
    }

    private static Fixture newFixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new RestGatewayCatalog(builder.build()), server);
    }

    @Test
    void descriptorsCallsTheCatalogEndpointAndParsesTheView() {
        Fixture f = newFixture();
        f.server().expect(requestTo("/api/gateway/commands/descriptors"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"commandType":"EditGoal","inputType":"EditGoalInput","title":"Edit Goal",
                          "description":null,"write":true,"authorizationHint":null}]
                        """, MediaType.APPLICATION_JSON));

        List<CommandInfo> commands = f.catalog().descriptors();

        assertThat(commands).singleElement().satisfies(c -> {
            assertThat(c.commandType()).isEqualTo("EditGoal");
            assertThat(c.inputType()).isEqualTo("EditGoalInput");
            assertThat(c.title()).isEqualTo("Edit Goal");
            assertThat(c.write()).isTrue();
        });
        f.server().verify();
    }

    @Test
    void emptyListWhenServerExposesNoWriteCommands() {
        Fixture f = newFixture();
        f.server().expect(requestTo("/api/gateway/commands/descriptors"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(f.catalog().descriptors()).isEmpty();
        f.server().verify();
    }
}
