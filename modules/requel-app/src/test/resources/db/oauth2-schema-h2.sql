-- OAuth 2.1 Authorization Server schema for H2 tests (issue #83).
--
-- Tests run on H2 (MySQL mode) with Flyway DISABLED and Hibernate ddl-auto=create-drop
-- (see application-test.properties). The oauth2_* tables are managed by Spring Authorization
-- Server via JdbcTemplate, NOT by JPA, so Hibernate does not create them and Flyway does not run.
-- Slice 5's AS integration tests must therefore create these tables explicitly, e.g.:
--
--   @Sql(scripts = "/db/oauth2-schema-h2.sql", executionPhase = BEFORE_TEST_CLASS)
--
-- or by pointing a test slice at spring.sql.init.schema-locations. This script is intentionally
-- NOT wired into the global test properties so it cannot affect the existing suite.
--
-- DDL mirrors V12__oauth2_authorization_server.sql (canonical Spring AS 1.5.x schema); it is
-- valid on H2 in MySQL mode.

CREATE TABLE IF NOT EXISTS oauth2_registered_client (
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

CREATE TABLE IF NOT EXISTS oauth2_authorization (
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

CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorities VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);
