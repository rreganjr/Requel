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
package com.rreganjr.requel.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.nimbusds.jose.jwk.RSAKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * Unit test for the OAuth AS signing-key resolution (issue #105):
 * {@link AuthorizationServerConfig#buildSigningKey}.
 *
 * <p>Covers both paths without booting the authorization server:
 * <ul>
 *   <li><b>Keystore configured</b> — the key loads, the JWK {@code kid} is the alias, and the key is
 *       stable across two loads (simulating an app restart), which is what lets tokens survive a
 *       restart.</li>
 *   <li><b>No keystore</b> — an ephemeral key is generated with a random {@code kid} that differs on
 *       each call (the pre-#105 dev/test/CI behavior).</li>
 * </ul>
 *
 * <p>The test keystore is generated at runtime with {@code keytool} into a {@link TempDir}, so no
 * keystore is committed to source control (consistent with the credential guidance in
 * {@code doc/105-keystore-credentials.md}). The test is skipped if {@code keytool} is not present in
 * the running JDK.
 */
class OAuthSigningKeyTest {

    private static final String ALIAS = "requel-oauth-signing";
    private static final String STORE_PASS = "changeit";

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    // ---- keystore-configured path ---------------------------------------------------------------

    @Test
    void loadsKeyFromKeystoreWithAliasAsStableKid(@TempDir Path dir) throws Exception {
        String location = generatePkcs12(dir).toUri().toString(); // file: resource

        RSAKey key = AuthorizationServerConfig.buildSigningKey(
                location, "PKCS12", STORE_PASS, ALIAS, STORE_PASS, resourceLoader);

        assertThat(key.getKeyID()).isEqualTo(ALIAS);
        assertThat(key.toRSAPrivateKey()).isNotNull();
        assertThat(key.toRSAPublicKey().getModulus().bitLength()).isGreaterThanOrEqualTo(2048);
    }

    @Test
    void keystoreKeyIsStableAcrossReloads(@TempDir Path dir) throws Exception {
        String location = generatePkcs12(dir).toUri().toString();

        RSAKey first = AuthorizationServerConfig.buildSigningKey(
                location, "PKCS12", STORE_PASS, ALIAS, STORE_PASS, resourceLoader);
        RSAKey second = AuthorizationServerConfig.buildSigningKey(
                location, "PKCS12", STORE_PASS, ALIAS, STORE_PASS, resourceLoader);

        // Two app starts against the same keystore: identical kid + public key, so a token signed
        // before a restart still validates after it (issue #105 acceptance criterion).
        assertThat(second.getKeyID()).isEqualTo(first.getKeyID());
        assertThat(second.toRSAPublicKey().getModulus()).isEqualTo(first.toRSAPublicKey().getModulus());
    }

    @Test
    void defaultsKeystoreTypeToPkcs12AndKeyPasswordToStorePassword(@TempDir Path dir) throws Exception {
        String location = generatePkcs12(dir).toUri().toString();

        // Blank storeType -> PKCS12; blank keyPassword -> falls back to the store password.
        RSAKey key = AuthorizationServerConfig.buildSigningKey(
                location, "", STORE_PASS, ALIAS, "", resourceLoader);

        assertThat(key.getKeyID()).isEqualTo(ALIAS);
        assertThat(key.toRSAPrivateKey()).isNotNull();
    }

    @Test
    void keystoreLocationWithoutAliasFails(@TempDir Path dir) throws Exception {
        String location = generatePkcs12(dir).toUri().toString();

        assertThatThrownBy(() -> AuthorizationServerConfig.buildSigningKey(
                location, "PKCS12", STORE_PASS, "", STORE_PASS, resourceLoader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("key-alias");
    }

    @Test
    void unknownAliasFails(@TempDir Path dir) throws Exception {
        String location = generatePkcs12(dir).toUri().toString();

        assertThatThrownBy(() -> AuthorizationServerConfig.buildSigningKey(
                location, "PKCS12", STORE_PASS, "no-such-alias", STORE_PASS, resourceLoader))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingKeystoreFileFails() {
        assertThatThrownBy(() -> AuthorizationServerConfig.buildSigningKey(
                "file:/does/not/exist.p12", "PKCS12", STORE_PASS, ALIAS, STORE_PASS, resourceLoader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to load OAuth signing key");
    }

    // ---- ephemeral fallback path ----------------------------------------------------------------

    @Test
    void generatesEphemeralKeyWhenNoLocation() throws Exception {
        RSAKey key = AuthorizationServerConfig.buildSigningKey(
                "", "PKCS12", "", "", "", resourceLoader);

        assertThat(key.toRSAPrivateKey()).isNotNull();
        assertThat(key.toRSAPublicKey().getModulus().bitLength()).isGreaterThanOrEqualTo(2048);
        assertThat(key.getKeyID()).isNotBlank();
    }

    @Test
    void ephemeralKeysDifferBetweenCalls() {
        RSAKey first = AuthorizationServerConfig.buildSigningKey(
                null, null, null, null, null, resourceLoader);
        RSAKey second = AuthorizationServerConfig.buildSigningKey(
                null, null, null, null, null, resourceLoader);

        // Each "restart" mints a new key -> different kid. This is exactly the caveat #105 removes.
        assertThat(second.getKeyID()).isNotEqualTo(first.getKeyID());
    }

    // ---- helper: throwaway PKCS12 via keytool (nothing committed to source control) --------------

    private static Path generatePkcs12(Path dir) throws Exception {
        Path keytool = Paths.get(System.getProperty("java.home"), "bin",
                isWindows() ? "keytool.exe" : "keytool");
        assumeTrue(Files.isExecutable(keytool), "keytool not available in this JDK; skipping");

        Path keystore = dir.resolve("oauth-signing.p12");
        Process process = new ProcessBuilder(
                keytool.toString(),
                "-genkeypair",
                "-alias", ALIAS,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-dname", "CN=Requel OAuth Signing (test)",
                "-validity", "3650",
                "-keystore", keystore.toString(),
                "-storetype", "PKCS12",
                "-storepass", STORE_PASS,
                "-keypass", STORE_PASS)
                .redirectErrorStream(true)
                .start();

        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("keytool timed out generating the test keystore");
        }
        if (process.exitValue() != 0 || !Files.exists(keystore)) {
            String output = new String(process.getInputStream().readAllBytes());
            throw new IllegalStateException("keytool failed to generate the test keystore:\n" + output);
        }
        return keystore;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
