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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link ApiTokenService} (issue #73, Slice 2). Lives in requel-app because
 * service-impl carries no test dependencies; the service classes are public, so they are reachable
 * across the module boundary. Plain Mockito unit test — no Spring context.
 */
class ApiTokenServiceTest {

	private final ApiTokenRepository repository = mock(ApiTokenRepository.class);
	private final ApiTokenService service = new ApiTokenService(repository);

	@Test
	void createMintsPrefixedTokenAndStoresOnlyTheHash() {
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		ApiTokenService.IssuedToken issued = service.create(7L, "ci", 30);

		assertThat(issued.plaintext()).startsWith(ApiTokenService.PREFIX);

		ArgumentCaptor<ApiToken> captor = ArgumentCaptor.forClass(ApiToken.class);
		verify(repository).save(captor.capture());
		ApiToken saved = captor.getValue();
		assertThat(saved.getTokenHash()).isEqualTo(ApiTokenService.hash(issued.plaintext()));
		assertThat(saved.getTokenHash()).isNotEqualTo(issued.plaintext()); // hash, not plaintext
		assertThat(saved.getOwnerUserId()).isEqualTo(7L);
		assertThat(saved.getName()).isEqualTo("ci");
		assertThat(saved.getExpiresAt()).isNotNull();
		assertThat(saved.isRevoked()).isFalse();
	}

	@Test
	void nullExpiryMeansNeverExpires() {
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		ArgumentCaptor<ApiToken> captor = ArgumentCaptor.forClass(ApiToken.class);

		service.create(1L, "forever", null);

		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getExpiresAt()).isNull();
	}

	@Test
	void isApiTokenDetectsThePrefixOnly() {
		assertThat(ApiTokenService.isApiToken("reqpat_abc123")).isTrue();
		assertThat(ApiTokenService.isApiToken("eyJhbGciOi.jwt.token")).isFalse();
		assertThat(ApiTokenService.isApiToken(null)).isFalse();
	}

	@Test
	void hashIs64HexCharsAndDeterministic() {
		String hash = ApiTokenService.hash("reqpat_xyz");
		assertThat(hash).hasSize(64).matches("[0-9a-f]+");
		assertThat(hash).isEqualTo(ApiTokenService.hash("reqpat_xyz"));
		assertThat(hash).isNotEqualTo(ApiTokenService.hash("reqpat_other"));
	}

	@Test
	void revokeOnlyAffectsTheCallersOwnToken() {
		ApiToken othersToken = new ApiToken(99L, "theirs", "h", Instant.now(), null);
		when(repository.findById(5L)).thenReturn(Optional.of(othersToken));

		assertThat(service.revoke(7L, 5L)).isFalse(); // user 7 cannot revoke user 99's token
		assertThat(othersToken.isRevoked()).isFalse();
		verify(repository, never()).save(any());

		ApiToken ownToken = new ApiToken(7L, "mine", "h2", Instant.now(), null);
		when(repository.findById(6L)).thenReturn(Optional.of(ownToken));

		assertThat(service.revoke(7L, 6L)).isTrue();
		assertThat(ownToken.isRevoked()).isTrue();
	}

	@Test
	void revokeMissingTokenReturnsFalse() {
		when(repository.findById(404L)).thenReturn(Optional.empty());
		assertThat(service.revoke(7L, 404L)).isFalse();
	}
}
