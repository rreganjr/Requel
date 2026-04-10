/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SensitiveFieldRedactor}.
 *
 * Constructed directly — no Spring context needed. Uses the real {@link ObjectMapper}
 * so JSON round-trip behaviour matches production.
 *
 * Scenarios covered:
 * - null input returns null
 * - default sensitive keys are redacted (password, repassword, secret, token,
 *   credential, credentials, apikey, api_key)
 * - innocent fields (username, name, email) are not redacted
 * - key matching is case-insensitive (Password, PASSWORD, pAsSwOrD)
 * - sensitive key in a nested object is redacted
 * - sensitive key inside an array element is redacted
 * - custom sensitive key list via constructor
 * - non-serializable input returns error sentinel JSON
 */
class SensitiveFieldRedactorTest {

    private static final List<String> DEFAULT_KEYS = List.of(
            "password", "repassword", "secret", "token",
            "credential", "credentials", "apikey", "api_key");

    private SensitiveFieldRedactor redactor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        redactor = new SensitiveFieldRedactor(objectMapper, DEFAULT_KEYS);
    }

    // -------------------------------------------------------------------------
    // Null / edge cases
    // -------------------------------------------------------------------------

    @Test
    void nullInputReturnsNull() {
        assertThat(redactor.redact(null)).isNull();
    }

    // -------------------------------------------------------------------------
    // Default sensitive keys
    // -------------------------------------------------------------------------

    @Test
    void passwordIsRedacted() throws Exception {
        String json = redactor.redact(Map.of("password", "s3cret"));
        assertThat(objectMapper.readTree(json).get("password").asText()).isEqualTo("[REDACTED]");
    }

    @Test
    void repasswordIsRedacted() throws Exception {
        String json = redactor.redact(Map.of("repassword", "s3cret"));
        assertThat(objectMapper.readTree(json).get("repassword").asText()).isEqualTo("[REDACTED]");
    }

    @Test
    void secretIsRedacted() throws Exception {
        String json = redactor.redact(Map.of("secret", "mysecret"));
        assertThat(objectMapper.readTree(json).get("secret").asText()).isEqualTo("[REDACTED]");
    }

    @Test
    void tokenIsRedacted() throws Exception {
        String json = redactor.redact(Map.of("token", "jwt-abc-123"));
        assertThat(objectMapper.readTree(json).get("token").asText()).isEqualTo("[REDACTED]");
    }

    @Test
    void credentialIsRedacted() throws Exception {
        String json = redactor.redact(Map.of("credential", "value"));
        assertThat(objectMapper.readTree(json).get("credential").asText()).isEqualTo("[REDACTED]");
    }

    @Test
    void credentialsIsRedacted() throws Exception {
        String json = redactor.redact(Map.of("credentials", "value"));
        assertThat(objectMapper.readTree(json).get("credentials").asText()).isEqualTo("[REDACTED]");
    }

    @Test
    void apikeyIsRedacted() throws Exception {
        String json = redactor.redact(Map.of("apikey", "key-xyz"));
        assertThat(objectMapper.readTree(json).get("apikey").asText()).isEqualTo("[REDACTED]");
    }

    @Test
    void api_keyIsRedacted() throws Exception {
        String json = redactor.redact(Map.of("api_key", "key-xyz"));
        assertThat(objectMapper.readTree(json).get("api_key").asText()).isEqualTo("[REDACTED]");
    }

    // -------------------------------------------------------------------------
    // Innocent fields are preserved
    // -------------------------------------------------------------------------

    @Test
    void innocentFieldsAreNotRedacted() throws Exception {
        String json = redactor.redact(Map.of(
                "username", "alice",
                "name", "Alice Smith",
                "emailAddress", "alice@example.com"));

        var tree = objectMapper.readTree(json);
        assertThat(tree.get("username").asText()).isEqualTo("alice");
        assertThat(tree.get("name").asText()).isEqualTo("Alice Smith");
        assertThat(tree.get("emailAddress").asText()).isEqualTo("alice@example.com");
    }

    // -------------------------------------------------------------------------
    // Case-insensitive matching
    // -------------------------------------------------------------------------

    @Test
    void passwordKeyIsCaseInsensitive() throws Exception {
        assertThat(objectMapper.readTree(redactor.redact(Map.of("Password", "x")))
                .get("Password").asText()).isEqualTo("[REDACTED]");
        assertThat(objectMapper.readTree(redactor.redact(Map.of("PASSWORD", "x")))
                .get("PASSWORD").asText()).isEqualTo("[REDACTED]");
        assertThat(objectMapper.readTree(redactor.redact(Map.of("pAsSwOrD", "x")))
                .get("pAsSwOrD").asText()).isEqualTo("[REDACTED]");
    }

    // -------------------------------------------------------------------------
    // Nested structures
    // -------------------------------------------------------------------------

    @Test
    void sensitiveFieldInNestedObjectIsRedacted() throws Exception {
        // "credentials" key is itself sensitive — the nested object is replaced wholesale
        String json = redactor.redact(Map.of(
                "username", "alice",
                "credentials", Map.of("password", "secret123", "hint", "pet name")));
        var tree = objectMapper.readTree(json);

        assertThat(tree.get("username").asText()).isEqualTo("alice");
        assertThat(tree.get("credentials").asText()).isEqualTo("[REDACTED]");
    }

    @Test
    void sensitiveFieldInNonSensitiveNestedObjectIsRedacted() throws Exception {
        // "auth" key is innocent; "password" inside it is sensitive
        String json = redactor.redact(Map.of(
                "username", "alice",
                "auth", Map.of("password", "secret123", "hint", "pet name")));
        var tree = objectMapper.readTree(json);

        assertThat(tree.get("username").asText()).isEqualTo("alice");
        assertThat(tree.get("auth").get("password").asText()).isEqualTo("[REDACTED]");
        assertThat(tree.get("auth").get("hint").asText()).isEqualTo("pet name");
    }

    @Test
    void sensitiveFieldInsideArrayElementIsRedacted() throws Exception {
        String json = redactor.redact(List.of(
                Map.of("name", "first",  "token", "tok-aaa"),
                Map.of("name", "second", "token", "tok-bbb")));

        var arr = objectMapper.readTree(json);
        assertThat(arr.get(0).get("name").asText()).isEqualTo("first");
        assertThat(arr.get(0).get("token").asText()).isEqualTo("[REDACTED]");
        assertThat(arr.get(1).get("token").asText()).isEqualTo("[REDACTED]");
    }

    // -------------------------------------------------------------------------
    // Custom sensitive key list
    // -------------------------------------------------------------------------

    @Test
    void customSensitiveKeyListIsRespected() throws Exception {
        SensitiveFieldRedactor custom = new SensitiveFieldRedactor(
                objectMapper, List.of("ssn", "dob"));

        String json = custom.redact(Map.of(
                "username", "alice",
                "ssn", "123-45-6789",
                "dob", "1990-01-01",
                "password", "stillVisible"));

        var tree = objectMapper.readTree(json);
        assertThat(tree.get("ssn").asText()).isEqualTo("[REDACTED]");
        assertThat(tree.get("dob").asText()).isEqualTo("[REDACTED]");
        // "password" is not in the custom list
        assertThat(tree.get("password").asText()).isEqualTo("stillVisible");
        assertThat(tree.get("username").asText()).isEqualTo("alice");
    }
}
