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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A user-minted personal access token (PAT) for non-interactive / static-bearer clients (issue
 * #73). The token is an opaque high-entropy string returned to the user once at creation; only its
 * SHA-256 hash is stored here, so the plaintext cannot be recovered. {@code JwtAuthenticationFilter}
 * (Slice 3) hashes a presented PAT, looks it up by {@link #getTokenHash()} on every request — giving
 * immediate revocation — checks {@link #isRevoked()} / {@link #getExpiresAt()}, then resolves the
 * owning user and loads authorities live (so role changes take effect immediately). The token only
 * establishes the triggering user; authorization (stakeholder permissions) and audit are unchanged.
 */
@Entity
@Table(name = "api_tokens")
public class ApiToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Owning user's id. No DB FK (mirrors command_audit_log.user_id); resolved via the user store. */
	@Column(name = "owner_user_id", nullable = false)
	private Long ownerUserId;

	@Column(name = "name", nullable = false, length = 120)
	private String name;

	/** SHA-256 hex of the opaque token (64 chars). Unique so lookup is a single indexed hit. */
	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "last_used_at")
	private Instant lastUsedAt;

	/** Null = never expires. */
	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "revoked", nullable = false)
	private boolean revoked;

	protected ApiToken() {
		// for JPA
	}

	public ApiToken(Long ownerUserId, String name, String tokenHash, Instant createdAt,
			Instant expiresAt) {
		this.ownerUserId = ownerUserId;
		this.name = name;
		this.tokenHash = tokenHash;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
		this.revoked = false;
	}

	public Long getId() {
		return id;
	}

	public Long getOwnerUserId() {
		return ownerUserId;
	}

	public String getName() {
		return name;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getLastUsedAt() {
		return lastUsedAt;
	}

	public void setLastUsedAt(Instant lastUsedAt) {
		this.lastUsedAt = lastUsedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public boolean isRevoked() {
		return revoked;
	}

	public void setRevoked(boolean revoked) {
		this.revoked = revoked;
	}

	/** True if the token is currently usable (not revoked and not past its expiry). */
	public boolean isActive(Instant now) {
		return !revoked && (expiresAt == null || expiresAt.isAfter(now));
	}
}
