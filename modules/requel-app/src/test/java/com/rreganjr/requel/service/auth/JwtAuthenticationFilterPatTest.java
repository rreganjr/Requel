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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tests the personal-access-token branch of {@link JwtAuthenticationFilter} (issue #73, Slice 3):
 * a valid PAT authenticates as the owning user with live authorities; revoked / expired / unknown
 * tokens leave the request unauthenticated.
 */
class JwtAuthenticationFilterPatTest {

	private final JwtService jwtService = mock(JwtService.class);
	private final ApiTokenRepository tokenRepository = mock(ApiTokenRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final UserDtoMapper userDtoMapper = mock(UserDtoMapper.class);

	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
			jwtService, tokenRepository, userRepository, userDtoMapper);

	private static final String PLAINTEXT = ApiTokenService.PREFIX + "unit-test-token";
	private static final String HASH = ApiTokenService.hash(PLAINTEXT);

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	private Authentication runWith(ApiToken token) throws Exception {
		when(tokenRepository.findByTokenHash(HASH)).thenReturn(Optional.ofNullable(token));
		User user = mock(User.class);
		lenient().when(user.getUsername()).thenReturn("alice");
		lenient().when(userRepository.findUserById(7L)).thenReturn(user);
		lenient().when(userDtoMapper.getRoleStrings(user)).thenReturn(List.of("ProjectUserRole"));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + PLAINTEXT);
		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
		return SecurityContextHolder.getContext().getAuthentication();
	}

	@Test
	void validTokenAuthenticatesAsOwnerWithLiveAuthorities() throws Exception {
		ApiToken active = new ApiToken(7L, "ci", HASH, Instant.now(), null);

		Authentication auth = runWith(active);

		assertThat(auth).isNotNull();
		assertThat(auth.getName()).isEqualTo("alice");
		assertThat(auth.getAuthorities()).extracting(Object::toString)
				.contains("ROLE_ProjectUserRole");
	}

	@Test
	void revokedTokenIsRejected() throws Exception {
		ApiToken revoked = new ApiToken(7L, "ci", HASH, Instant.now(), null);
		revoked.setRevoked(true);

		assertThat(runWith(revoked)).isNull();
	}

	@Test
	void expiredTokenIsRejected() throws Exception {
		ApiToken expired = new ApiToken(7L, "ci", HASH, Instant.now().minus(2, ChronoUnit.DAYS),
				Instant.now().minus(1, ChronoUnit.DAYS));

		assertThat(runWith(expired)).isNull();
	}

	@Test
	void unknownTokenIsRejected() throws Exception {
		assertThat(runWith(null)).isNull();
	}
}
