-- OAuth 2.1 Authorization Server persistence (issue #83, Slice 1).
--
-- Tables for Spring Authorization Server's JDBC services:
--   * oauth2_registered_client         -> JdbcRegisteredClientRepository (incl. DCR-created clients)
--   * oauth2_authorization             -> JdbcOAuth2AuthorizationService (auth codes, access/refresh
--                                         tokens, OIDC id tokens, device/user codes)
--   * oauth2_authorization_consent     -> JdbcOAuth2AuthorizationConsentService (per user+client)
--
-- Schema follows the canonical Spring Authorization Server DDL (1.5.x), adjusted for MySQL:
-- BLOB/TEXT columns carry no explicit DEFAULT (MySQL rejects defaults on BLOB/TEXT; NULL is the
-- implicit default for nullable columns). Valid on H2 in MySQL mode as well.

CREATE TABLE oauth2_registered_client (
    id VARCHAR(100) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    client_id_issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret VARCHAR(200) DEFAULT NULL,
    client_secret_expires_at TIMESTAMP DEFAULT NULL,
    client_name VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types VARCHAR(1000) NOT NULL,
    redirect_uris VARCHAR(1000) DEFAULT NULL,
    post_logout_redirect_uris VARCHAR(1000) DEFAULT NULL,
    scopes VARCHAR(1000) NOT NULL,
    client_settings VARCHAR(2000) NOT NULL,
    token_settings VARCHAR(2000) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE oauth2_authorization (
    id VARCHAR(100) NOT NULL,
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorization_grant_type VARCHAR(100) NOT NULL,
    authorized_scopes VARCHAR(1000) DEFAULT NULL,
    attributes BLOB,
    state VARCHAR(500) DEFAULT NULL,
    authorization_code_value BLOB,
    authorization_code_issued_at TIMESTAMP DEFAULT NULL,
    authorization_code_expires_at TIMESTAMP DEFAULT NULL,
    authorization_code_metadata BLOB,
    access_token_value BLOB,
    access_token_issued_at TIMESTAMP DEFAULT NULL,
    access_token_expires_at TIMESTAMP DEFAULT NULL,
    access_token_metadata BLOB,
    access_token_type VARCHAR(100) DEFAULT NULL,
    access_token_scopes VARCHAR(1000) DEFAULT NULL,
    oidc_id_token_value BLOB,
    oidc_id_token_issued_at TIMESTAMP DEFAULT NULL,
    oidc_id_token_expires_at TIMESTAMP DEFAULT NULL,
    oidc_id_token_metadata BLOB,
    refresh_token_value BLOB,
    refresh_token_issued_at TIMESTAMP DEFAULT NULL,
    refresh_token_expires_at TIMESTAMP DEFAULT NULL,
    refresh_token_metadata BLOB,
    user_code_value BLOB,
    user_code_issued_at TIMESTAMP DEFAULT NULL,
    user_code_expires_at TIMESTAMP DEFAULT NULL,
    user_code_metadata BLOB,
    device_code_value BLOB,
    device_code_issued_at TIMESTAMP DEFAULT NULL,
    device_code_expires_at TIMESTAMP DEFAULT NULL,
    device_code_metadata BLOB,
    PRIMARY KEY (id)
);

CREATE TABLE oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorities VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);
