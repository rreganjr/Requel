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

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * An OAuth access token issued before the user's last password change is refused (issue #446);
 * one issued in the same second as the change, or later, is accepted.
 */
class McpJwtAuthenticationConverterTest {

    private static final Instant CHANGED = Instant.parse("2026-11-02T06:00:00Z");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserDtoMapper userDtoMapper = mock(UserDtoMapper.class);
    private final McpJwtAuthenticationConverter converter =
            new McpJwtAuthenticationConverter(userRepository, userDtoMapper);

    private void userChangedPasswordAt(Instant changedAt) {
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("ron");
        when(user.getPasswordChangedAt()).thenReturn(changedAt);
        when(userRepository.findUserByUsername("ron")).thenReturn(user);
        when(userDtoMapper.getRoleStrings(user)).thenReturn(List.of("ProjectUserRole"));
    }

    private static Jwt issuedAt(Instant iat) {
        return Jwt.withTokenValue("t").header("alg", "RS256").subject("ron").issuedAt(iat)
                .expiresAt(iat.plusSeconds(3600)).build();
    }

    @Test
    void aTokenFromBeforeThePasswordChangeIsRefused() {
        userChangedPasswordAt(CHANGED);
        assertThatThrownBy(() -> converter.convert(issuedAt(CHANGED.minusSeconds(1))))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void aTokenFromTheSameSecondOrLaterIsAccepted() {
        userChangedPasswordAt(CHANGED);
        assertThat(converter.convert(issuedAt(CHANGED.plusMillis(400)))).isNotNull();
        assertThat(converter.convert(issuedAt(CHANGED.plusSeconds(60)))).isNotNull();
    }

    @Test
    void aUserWhosePasswordNeverChangedIsUnaffected() {
        userChangedPasswordAt(null);
        assertThat(converter.convert(issuedAt(CHANGED.minusSeconds(3600)))).isNotNull();
    }
}
