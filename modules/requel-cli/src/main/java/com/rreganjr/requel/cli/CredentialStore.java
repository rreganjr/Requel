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

import com.rreganjr.requel.gateway.rest.OAuthTokens;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Properties;

/**
 * Persists {@code requel} bearer tokens (PATs, #73) per server URL, so a token survives across
 * invocations without living in {@code --token}/{@code REQUEL_TOKEN}. Stored as a simple properties
 * file (URL → token) under {@code ~/.config/requel/credentials} (override the directory with
 * {@code REQUEL_CONFIG_DIR}), created with owner-only permissions on POSIX filesystems. Never a
 * checked-in location.
 */
public class CredentialStore {

    private final Path dir;
    private final Path file;

    public CredentialStore() {
        this(defaultDir());
    }

    /** For tests / custom locations. */
    public CredentialStore(Path dir) {
        this.dir = dir;
        this.file = dir.resolve("credentials");
    }

    private static Path defaultDir() {
        String override = System.getenv("REQUEL_CONFIG_DIR");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of(System.getProperty("user.home"), ".config", "requel");
    }

    /** @return the stored token for {@code baseUrl}, or {@code null} if none. */
    public synchronized String find(String baseUrl) {
        String value = load().getProperty(key(baseUrl));
        return (value == null || value.isBlank()) ? null : value;
    }

    /** Store (or replace) the token for {@code baseUrl}. */
    public synchronized void save(String baseUrl, String token) {
        Properties props = load();
        props.setProperty(key(baseUrl), token);
        write(props);
    }

    /** Remove any stored credentials for {@code baseUrl} — both a PAT and OAuth tokens. */
    public synchronized void delete(String baseUrl) {
        Properties props = load();
        boolean changed = props.remove(key(baseUrl)) != null;
        for (String suffix : OAUTH_SUFFIXES) {
            changed |= props.remove(oauthKey(baseUrl, suffix)) != null;
        }
        if (changed) {
            write(props);
        }
    }

    /** Store (or replace) the OAuth tokens for {@code baseUrl}. */
    public synchronized void saveOAuth(String baseUrl, OAuthTokens tokens) {
        Properties props = load();
        props.setProperty(oauthKey(baseUrl, "access"), tokens.accessToken());
        setOrRemove(props, oauthKey(baseUrl, "refresh"), tokens.refreshToken());
        setOrRemove(props, oauthKey(baseUrl, "scope"), tokens.scope());
        setOrRemove(props, oauthKey(baseUrl, "expiresAt"),
                tokens.expiresAt() == null ? null : Long.toString(tokens.expiresAt().getEpochSecond()));
        write(props);
    }

    /** @return the stored OAuth tokens for {@code baseUrl}, or {@code null} if none. */
    public synchronized OAuthTokens findOAuth(String baseUrl) {
        Properties props = load();
        String access = props.getProperty(oauthKey(baseUrl, "access"));
        if (access == null || access.isBlank()) {
            return null;
        }
        String expiresAt = props.getProperty(oauthKey(baseUrl, "expiresAt"));
        return new OAuthTokens(access,
                props.getProperty(oauthKey(baseUrl, "refresh")),
                props.getProperty(oauthKey(baseUrl, "scope")),
                expiresAt == null ? null : Instant.ofEpochSecond(Long.parseLong(expiresAt)));
    }

    /** The credentials file location (for messages/tests). */
    public Path file() {
        return file;
    }

    private static final String[] OAUTH_SUFFIXES = {"access", "refresh", "scope", "expiresAt"};

    private static String key(String baseUrl) {
        return baseUrl == null ? "" : baseUrl.trim();
    }

    /** OAuth keys are namespaced ({@code oauth.<url>.<field>}) so they never collide with a PAT key. */
    private static String oauthKey(String baseUrl, String suffix) {
        return "oauth." + key(baseUrl) + "." + suffix;
    }

    private static void setOrRemove(Properties props, String key, String value) {
        if (value == null || value.isBlank()) {
            props.remove(key);
        } else {
            props.setProperty(key, value);
        }
    }

    private Properties load() {
        Properties props = new Properties();
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read " + file, e);
            }
        }
        return props;
    }

    private void write(Properties props) {
        try {
            Files.createDirectories(dir);
            restrict(dir, "rwx------");
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "Requel CLI credentials — do not commit");
            }
            restrict(file, "rw-------");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write " + file, e);
        }
    }

    /** Best-effort owner-only permissions; silently skipped on non-POSIX filesystems (e.g. Windows). */
    private static void restrict(Path path, String perms) {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(perms));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Non-POSIX filesystem — rely on the platform's default ACLs.
        }
    }
}
