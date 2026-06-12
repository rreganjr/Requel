-- Personal access tokens (PATs) for non-interactive / static-bearer clients (issue #73).
-- Opaque tokens; only the SHA-256 hash is stored. Looked up by hash on every request so
-- revocation (revoked = true) and expiry take effect immediately. owner_user_id has no FK,
-- mirroring command_audit_log.user_id; it is resolved via the user store at validation time.
CREATE TABLE api_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_api_tokens_owner (owner_user_id)
);
