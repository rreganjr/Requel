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
package com.rreganjr.requel.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class LoopbackCallbackServerTest {

    private static void hit(String url) throws Exception {
        HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.discarding());
    }

    @Test
    void capturesCodeAndValidatesState() throws Exception {
        try (LoopbackCallbackServer server = LoopbackCallbackServer.start()) {
            assertThat(server.redirectUri()).startsWith("http://127.0.0.1:").endsWith("/callback");
            hit(server.redirectUri() + "?code=the-code&state=xyz");

            assertThat(server.awaitCode("xyz", 5, TimeUnit.SECONDS)).isEqualTo("the-code");
        }
    }

    @Test
    void rejectsStateMismatch() throws Exception {
        try (LoopbackCallbackServer server = LoopbackCallbackServer.start()) {
            hit(server.redirectUri() + "?code=the-code&state=wrong");

            assertThatThrownBy(() -> server.awaitCode("expected", 5, TimeUnit.SECONDS))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("State mismatch");
        }
    }

    @Test
    void surfacesAuthorizationServerError() throws Exception {
        try (LoopbackCallbackServer server = LoopbackCallbackServer.start()) {
            hit(server.redirectUri() + "?error=access_denied&error_description=nope");

            assertThatThrownBy(() -> server.awaitCode("xyz", 5, TimeUnit.SECONDS))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("access_denied");
        }
    }
}
