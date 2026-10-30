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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * Issue #381: the OAuth sign-in form refuses a user who must change their password, and
 * {@link PasswordChangeRequiredFilter} lets such a caller reach only login, /me and the change.
 */
class PasswordChangeRequiredTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ---- RequelUserAuthenticationProvider (the OAuth sign-in form) -------------------------------

    @Test
    void theOAuthFormRefusesAFlaggedUserWithTheRightPassword() {
        RequelUserAuthenticationProvider provider = provider(user(true, true));
        assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken("ron", "pw")))
                .isInstanceOf(CredentialsExpiredException.class);
    }

    @Test
    void aFlaggedUserWithTheWrongPasswordStillGetsBadCredentials() {
        RequelUserAuthenticationProvider provider = provider(user(true, false));
        assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken("ron", "pw")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void anUnflaggedUserSignsIn() {
        RequelUserAuthenticationProvider provider = provider(user(false, true));
        assertThat(provider.authenticate(new UsernamePasswordAuthenticationToken("ron", "pw")).getName())
                .isEqualTo("ron");
    }

    // ---- PasswordChangeRequiredFilter ------------------------------------------------------------

    @Test
    void aRestrictedCallerIsRefusedEverywhereElse() throws Exception {
        restricted();
        MockHttpServletResponse response = filter("GET", "/api/projects");
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains(PasswordChangeRequiredFilter.AUTHORITY);
    }

    @Test
    void aRestrictedCallerCanSignInSeeWhoTheyAreAndChangeThePassword() throws Exception {
        restricted();
        assertThat(filter("POST", "/api/auth/password").getStatus()).isEqualTo(200);
        assertThat(filter("GET", "/api/auth/me").getStatus()).isEqualTo(200);
        assertThat(filter("POST", "/api/auth/login").getStatus()).isEqualTo(200);
        assertThat(filter("GET", "/api/auth/password").getStatus()).isEqualTo(403);
    }

    @Test
    void anUnrestrictedCallerPassesThrough() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "ron", null, List.of()));
        assertThat(filter("GET", "/api/projects").getStatus()).isEqualTo(200);
    }

    @Test
    void anAnonymousRequestPassesThrough() throws Exception {
        assertThat(filter("GET", "/api/projects").getStatus()).isEqualTo(200);
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private static void restricted() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "ron", null, List.of(PasswordChangeRequiredFilter.GRANTED_AUTHORITY)));
    }

    private static MockHttpServletResponse filter(String method, String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new PasswordChangeRequiredFilter().doFilter(request, response, new MockFilterChain());
        return response;
    }

    private static User user(boolean flagged, boolean passwordMatches) {
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("ron");
        when(user.isPassword("pw")).thenReturn(passwordMatches);
        when(user.isMustChangePassword()).thenReturn(flagged);
        return user;
    }

    private static RequelUserAuthenticationProvider provider(User user) {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findUserByUsername("ron")).thenReturn(user);
        UserDtoMapper mapper = mock(UserDtoMapper.class);
        when(mapper.getRoleStrings(user)).thenReturn(List.of("ProjectUserRole"));
        return new RequelUserAuthenticationProvider(repository, mapper);
    }
}
