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
package com.rreganjr.requel.service.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rreganjr.AbstractIntegrationTestCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Guards the SPA-vs-authorization-server login routing (issue #83).
 *
 * <p>The Angular SPA owns the client-side route {@code /login} (its {@code LoginComponent}); the
 * authorization server's login page lives at {@code /oauth2/login}. An earlier version of the OAuth
 * work put the AS form login on {@code /login}, which shadowed the SPA page with Spring Security's
 * generated login form and broke the auth Playwright e2e tests. That collision was invisible to
 * {@code mvn verify} (it only runs the backend, not the browser suite). These MockMvc checks run in
 * the normal build, so the regression can't come back silently:
 *
 * <ul>
 *   <li>{@code GET /login} must NOT be intercepted by a security chain — it forwards to the SPA's
 *       {@code index.html} via {@code SpaController} (asserted on the forwarded URL, so it holds even
 *       when the Angular build isn't present in the test classpath).</li>
 *   <li>{@code GET /oauth2/login} serves the authorization server's own login page.</li>
 * </ul>
 */
@AutoConfigureMockMvc
public class OAuthLoginRouteIT extends AbstractIntegrationTestCase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginRouteFallsThroughToTheAngularSpa() throws Exception {
        // No security chain matches /login, so SpaController forwards it to the SPA shell.
        // (Asserting the forwarded URL rather than a 200 keeps this independent of whether the
        // Angular build produced index.html in the test classpath.)
        mockMvc.perform(get("/login"))
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void authorizationServerLoginPageIsServedAtOauth2Login() throws Exception {
        mockMvc.perform(get("/oauth2/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sign in to Requel")));
    }
}
