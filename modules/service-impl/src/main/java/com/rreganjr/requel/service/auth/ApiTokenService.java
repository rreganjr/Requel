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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mints, lists, and revokes personal access tokens (issue #73, Slice 2), and provides the shared
 * token-format/hashing helpers the auth filter reuses (Slice 3). Tokens are opaque high-entropy
 * strings with a fixed prefix so the filter can distinguish a PAT from a login JWT; only the
 * SHA-256 hash is persisted, so the plaintext is shown once at creation and never recoverable.
 */
@Service
public class ApiTokenService {

	/** Identifies a Requel PAT (vs a login JWT) by the Authorization bearer value's prefix. */
	public static final String PREFIX = "reqpat_";

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int RANDOM_BYTES = 32;

	private final ApiTokenRepository repository;

	public ApiTokenService(ApiTokenRepository repository) {
		this.repository = repository;
	}

	/** The plaintext token (shown once) plus the persisted record. */
	public record IssuedToken(String plaintext, ApiToken token) {
	}

	/**
	 * Mint a new token for {@code ownerUserId}. The plaintext is returned once; only its hash is
	 * stored. {@code expiresInDays} null = never expires.
	 */
	@Transactional
	public IssuedToken create(Long ownerUserId, String name, Integer expiresInDays) {
		byte[] bytes = new byte[RANDOM_BYTES];
		RANDOM.nextBytes(bytes);
		String plaintext = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		Instant now = Instant.now();
		Instant expiresAt = (expiresInDays != null && expiresInDays > 0)
				? now.plus(expiresInDays, ChronoUnit.DAYS)
				: null;
		ApiToken token = new ApiToken(ownerUserId, name, hash(plaintext), now, expiresAt);
		return new IssuedToken(plaintext, repository.save(token));
	}

	@Transactional(readOnly = true)
	public List<ApiToken> list(Long ownerUserId) {
		return repository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
	}

	/**
	 * Revoke a token the caller owns. Returns false (not found / not owned) so the caller can map to
	 * 404 — never reveals another user's token. Revocation takes effect on the token's next request.
	 */
	@Transactional
	public boolean revoke(Long ownerUserId, Long tokenId) {
		Optional<ApiToken> found = repository.findById(tokenId);
		if (found.isEmpty() || !found.get().getOwnerUserId().equals(ownerUserId)) {
			return false;
		}
		ApiToken token = found.get();
		if (!token.isRevoked()) {
			token.setRevoked(true);
			repository.save(token);
		}
		return true;
	}

	/** @return true if a bearer credential is a Requel PAT (by prefix), not a login JWT. */
	public static boolean isApiToken(String credential) {
		return credential != null && credential.startsWith(PREFIX);
	}

	/** SHA-256 hex of the token plaintext; the only form persisted/compared. */
	public static String hash(String plaintext) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashed);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
