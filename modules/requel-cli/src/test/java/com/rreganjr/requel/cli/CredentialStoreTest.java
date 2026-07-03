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
package com.rreganjr.requel.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.rreganjr.requel.gateway.rest.OAuthTokens;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CredentialStoreTest {

    @Test
    void savesAndFindsTokenPerUrl(@TempDir Path dir) {
        CredentialStore store = new CredentialStore(dir);
        store.save("http://a:8080", "reqpat_A");
        store.save("http://b:9090", "reqpat_B");

        assertThat(store.find("http://a:8080")).isEqualTo("reqpat_A");
        assertThat(store.find("http://b:9090")).isEqualTo("reqpat_B");
        assertThat(store.find("http://unknown")).isNull();
    }

    @Test
    void deleteRemovesOnlyThatUrl(@TempDir Path dir) {
        CredentialStore store = new CredentialStore(dir);
        store.save("http://a:8080", "reqpat_A");
        store.save("http://b:9090", "reqpat_B");

        store.delete("http://a:8080");

        assertThat(store.find("http://a:8080")).isNull();
        assertThat(store.find("http://b:9090")).isEqualTo("reqpat_B");
    }

    @Test
    void persistsAcrossInstances(@TempDir Path dir) {
        new CredentialStore(dir).save("http://a:8080", "reqpat_A");
        assertThat(new CredentialStore(dir).find("http://a:8080")).isEqualTo("reqpat_A");
    }

    @Test
    void savesAndFindsOAuthTokensPerUrl(@TempDir Path dir) {
        CredentialStore store = new CredentialStore(dir);
        Instant expiry = Instant.now().plusSeconds(3600);
        store.saveOAuth("http://a:8080", new OAuthTokens("AT", "RT", "mcp", expiry));

        OAuthTokens found = new CredentialStore(dir).findOAuth("http://a:8080");
        assertThat(found).isNotNull();
        assertThat(found.accessToken()).isEqualTo("AT");
        assertThat(found.refreshToken()).isEqualTo("RT");
        assertThat(found.scope()).isEqualTo("mcp");
        assertThat(found.expiresAt().getEpochSecond()).isEqualTo(expiry.getEpochSecond());
        assertThat(store.findOAuth("http://unknown")).isNull();
    }

    @Test
    void oauthAndPatCoexistAndDeleteClearsBoth(@TempDir Path dir) {
        CredentialStore store = new CredentialStore(dir);
        store.save("http://a:8080", "reqpat_A");
        store.saveOAuth("http://a:8080", new OAuthTokens("AT", "RT", "mcp",
                Instant.now().plusSeconds(3600)));

        // Both are stored independently.
        assertThat(store.find("http://a:8080")).isEqualTo("reqpat_A");
        assertThat(store.findOAuth("http://a:8080")).isNotNull();

        store.delete("http://a:8080");

        assertThat(store.find("http://a:8080")).isNull();
        assertThat(store.findOAuth("http://a:8080")).isNull();
    }

    @Test
    void credentialsFileIsOwnerOnlyOnPosix(@TempDir Path dir) throws Exception {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        CredentialStore store = new CredentialStore(dir);
        store.save("http://a:8080", "reqpat_A");

        String perms = PosixFilePermissions.toString(Files.getPosixFilePermissions(store.file()));
        assertThat(perms).isEqualTo("rw-------");
    }
}
