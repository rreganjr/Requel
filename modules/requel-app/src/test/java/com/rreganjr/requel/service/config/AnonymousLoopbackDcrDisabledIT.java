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
package com.rreganjr.requel.service.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rreganjr.AbstractIntegrationTestCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regression guard for issue #238: with {@code requel.oauth.dcr.allow-anonymous-loopback} left at its
 * default ({@code false}), the anonymous-loopback filter is not wired, so behavior is identical to
 * #83 — an anonymous {@code POST /connect/register} is gated: the unauthenticated request is
 * redirected to the AS login page (302) rather than being handled anonymously. This class
 * inherits only the base test property source, so it shares the cached base context (no extra
 * application boot).
 */
@AutoConfigureMockMvc
public class AnonymousLoopbackDcrDisabledIT extends AbstractIntegrationTestCase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousRegistrationIsGatedWhenPropertyOff() throws Exception {
        String body = "{\"client_name\":\"codex\",\"redirect_uris\":[\"http://127.0.0.1:8899/callback\"]}";

        mockMvc.perform(post("/connect/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void metadataDoesNotAdvertiseRegistrationEndpointWhenDcrOff() throws Exception {
        // With DCR off (default), the RFC 8414 metadata must NOT advertise registration_endpoint,
        // so clients don't attempt a registration that would be gated.
        mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registration_endpoint").doesNotExist());
    }
}
