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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * A PKCE (RFC 7636) verifier/challenge pair for the {@code S256} method: {@code challenge =
 * base64url(sha256(verifier))}, no padding. The verifier is a high-entropy URL-safe random string.
 *
 * @param verifier  the {@code code_verifier} kept secret in-process and sent on token exchange
 * @param challenge the {@code code_challenge} sent on the authorization request
 */
public record Pkce(String verifier, String challenge) {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

    /** The {@code code_challenge_method} this class implements. */
    public static final String METHOD = "S256";

    /** Generate a fresh pair with a 32-byte (256-bit) random verifier. */
    public static Pkce generate() {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String verifier = B64URL.encodeToString(raw);
        return new Pkce(verifier, challengeFor(verifier));
    }

    /** {@code base64url(sha256(verifier))} — the S256 challenge for a given verifier. */
    static String challengeFor(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return B64URL.encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
