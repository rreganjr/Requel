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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * #445: a login JWT identifies its account by id, and the filter takes the username and roles
 * from that account on every request, never from the token's claims.
 */
class JwtAuthenticationFilterLoginTokenTest {

	private static final String SECRET = "unit-test-secret-at-least-32-bytes-long!!";

	private final JwtService jwtService = new JwtService(SECRET, 8);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final UserDtoMapper userDtoMapper = mock(UserDtoMapper.class);
	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
			jwtService, mock(ApiTokenRepository.class), userRepository, userDtoMapper);

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	private static User user(long id, String username) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(id);
		when(user.getUsername()).thenReturn(username);
		return user;
	}

	private Authentication runWith(String token) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);
		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
		return SecurityContextHolder.getContext().getAuthentication();
	}

	@Test
	void theUsernameAndRolesComeFromTheLiveAccount() throws Exception {
		String token = jwtService.generateToken(user(7L, "alice"), List.of("SystemAdminUserRole"), List.of());
		User live = user(7L, "alice-renamed");
		when(userRepository.findUserById(7L)).thenReturn(live);
		when(userDtoMapper.getRoleStrings(live)).thenReturn(List.of("ProjectUserRole"));

		Authentication auth = runWith(token);

		assertThat(auth).isNotNull();
		assertThat(auth.getName()).isEqualTo("alice-renamed");
		assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_ProjectUserRole");
	}

	@Test
	void aTokenForAMissingAccountIsRefused() throws Exception {
		String token = jwtService.generateToken(user(7L, "alice"), List.of("SystemAdminUserRole"), List.of());
		when(userRepository.findUserById(7L)).thenReturn(null);

		assertThat(runWith(token)).isNull();
	}

	@Test
	void aFailedAccountLookupIsRefused() throws Exception {
		String token = jwtService.generateToken(user(7L, "alice"), List.of("SystemAdminUserRole"), List.of());
		when(userRepository.findUserById(7L)).thenThrow(new RuntimeException("database down"));

		assertThat(runWith(token)).isNull();
	}

	/** A token issued before 2.0.3 has no user id; it is refused, never looked up by username. */
	@Test
	void aTokenWithoutAUserIdIsRefused() throws Exception {
		String token = Jwts.builder()
				.subject("admin")
				.claim("roles", List.of("SystemAdminUserRole"))
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 3_600_000L))
				.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
				.compact();

		assertThat(runWith(token)).isNull();
		verify(userRepository, never()).findUserByUsername(anyString());
	}

	@Test
	void aBadTokenIsRefused() throws Exception {
		assertThat(runWith("not-a-jwt")).isNull();
	}
}
