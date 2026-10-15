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

import com.rreganjr.requel.service.api.dto.ErrorResponse;
import com.rreganjr.requel.user.User;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Personal-access-token management (issue #73, Slice 2): a user creates, lists, and revokes their
 * own tokens. Conventional REST, not command dispatch, and strictly "own tokens only" — the owner
 * is always the authenticated caller (via {@link CurrentUserResolver}), never a path/body parameter,
 * so this is token self-service, not user administration. Mounted under {@code /api/auth/**}; the
 * existing security chain requires authentication for everything except {@code /api/auth/login}.
 */
@RestController
@RequestMapping("/api/auth/tokens")
public class ApiTokenController {

	/**
	 * The per-user UserRolePermission (on ProjectUserRole) that authorizes token management (#85).
	 * Referenced by name to avoid a service->project-jpa dependency; matches
	 * {@code ProjectUserRole.manageApiTokens}.
	 */
	private static final String MANAGE_API_TOKENS = "manageApiTokens";

	private final ApiTokenService tokenService;
	private final CurrentUserResolver currentUserResolver;
	private final UserDtoMapper userDtoMapper;

	public ApiTokenController(ApiTokenService tokenService, CurrentUserResolver currentUserResolver,
			UserDtoMapper userDtoMapper) {
		this.tokenService = tokenService;
		this.currentUserResolver = currentUserResolver;
		this.userDtoMapper = userDtoMapper;
	}

	/**
	 * Resolve the current caller, requiring the {@code manageApiTokens} permission. Works for both
	 * JWT and PAT callers (both load the user live). Returns the {@link User} when permitted, or a
	 * 403 {@link ResponseEntity} otherwise — callers must return the entity as-is when present.
	 */
	private ManageAuth requireManageApiTokens() {
		User user = currentUserResolver.resolve();
		if (!userDtoMapper.getPermissionStrings(user).contains(MANAGE_API_TOKENS)) {
			return new ManageAuth(null, ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(ErrorResponse.of("FORBIDDEN",
							"You do not have permission to manage API tokens")));
		}
		return new ManageAuth(user, null);
	}

	/** Either a permitted {@code user} or a {@code forbidden} 403 response (never both null). */
	private record ManageAuth(User user, ResponseEntity<?> forbidden) {
	}

	/** Create request: a display name and optional expiry in days (null/0 = never expires). */
	public record CreateApiTokenRequest(String name, Integer expiresInDays) {
	}

	/** Token metadata (never the secret); status is derived REVOKED / EXPIRED / ACTIVE. */
	public record ApiTokenDto(Long id, String name, Instant createdAt, Instant lastUsedAt,
			Instant expiresAt, String status) {
	}

	/** Create response: the one-time plaintext token plus its metadata. */
	public record CreateApiTokenResponse(String token, ApiTokenDto tokenInfo) {
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody(required = false) CreateApiTokenRequest request) {
		ManageAuth auth = requireManageApiTokens();
		if (auth.forbidden() != null) {
			return auth.forbidden();
		}
		if (request == null || request.name() == null || request.name().isBlank()) {
			return ResponseEntity.badRequest()
					.body(ErrorResponse.of("BAD_REQUEST", "Token name is required"));
		}
		Long ownerId = auth.user().getId();
		ApiTokenService.IssuedToken issued = tokenService.create(ownerId, request.name().trim(),
				request.expiresInDays());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new CreateApiTokenResponse(issued.plaintext(), toDto(issued.token())));
	}

	@GetMapping
	public ResponseEntity<?> list() {
		ManageAuth auth = requireManageApiTokens();
		if (auth.forbidden() != null) {
			return auth.forbidden();
		}
		Long ownerId = auth.user().getId();
		return ResponseEntity.ok(tokenService.list(ownerId).stream().map(this::toDto).toList());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> revoke(@PathVariable("id") Long id) {
		ManageAuth auth = requireManageApiTokens();
		if (auth.forbidden() != null) {
			return auth.forbidden();
		}
		Long ownerId = auth.user().getId();
		if (tokenService.revoke(ownerId, id)) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErrorResponse.of("NOT_FOUND", "Token not found"));
	}

	/**
	 * Hard-delete a token, removing the row (issue #87). Distinct from the revoke above: only a
	 * non-active token (already revoked or expired) may be deleted, so a live credential cannot be
	 * removed by accident — an active token returns 409 and must be revoked first. Own-tokens-only.
	 */
	@DeleteMapping("/{id}/permanent")
	public ResponseEntity<?> delete(@PathVariable("id") Long id) {
		ManageAuth auth = requireManageApiTokens();
		if (auth.forbidden() != null) {
			return auth.forbidden();
		}
		Long ownerId = auth.user().getId();
		switch (tokenService.delete(ownerId, id)) {
			case DELETED:
				return ResponseEntity.noContent().build();
			case NOT_DELETABLE:
				return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of("CONFLICT",
						"An active token must be revoked before it can be deleted"));
			case NOT_FOUND:
			default:
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ErrorResponse.of("NOT_FOUND", "Token not found"));
		}
	}

	private ApiTokenDto toDto(ApiToken token) {
		String status;
		if (token.isRevoked()) {
			status = "REVOKED";
		} else if (token.getExpiresAt() != null && !token.getExpiresAt().isAfter(Instant.now())) {
			status = "EXPIRED";
		} else {
			status = "ACTIVE";
		}
		return new ApiTokenDto(token.getId(), token.getName(), token.getCreatedAt(),
				token.getLastUsedAt(), token.getExpiresAt(), status);
	}
}
