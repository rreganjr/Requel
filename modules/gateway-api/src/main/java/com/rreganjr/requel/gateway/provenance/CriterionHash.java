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
package com.rreganjr.requel.gateway.provenance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;

/**
 * The stable reconciliation key for a requirement: a SHA-256 hex digest over a canonically
 * normalized form of the criterion text (issue #71).
 *
 * <p><b>Canonical normalization</b>, applied in this exact order so the key is reproducible across
 * this ticket and the ticket-4 similarity matcher:</p>
 * <ol>
 *   <li>trim leading/trailing whitespace,</li>
 *   <li>collapse each internal run of whitespace to a single space,</li>
 *   <li>lowercase (using {@link Locale#ROOT}), and</li>
 *   <li>strip trailing sentence punctuation and whitespace (the characters
 *       {@code . , ; : ! ?}).</li>
 * </ol>
 *
 * <p>Because the criterion text has no stable per-item id in free-text trackers, this hash is the
 * v1 provenance match key. A minor edit to the requirement changes the hash — the accepted v1
 * limitation documented in the plan.</p>
 *
 * <p>Stateless and thread-safe.</p>
 */
public final class CriterionHash {

    /** Trailing punctuation/whitespace stripped during normalization. */
    private static final String TRAILING_STRIP = "[.,;:!?\\s]+$";

    private CriterionHash() {
    }

    /**
     * Canonically normalize {@code criterionText} per the class contract. Exposed for reuse by the
     * ticket-4 matcher and for test assertions.
     *
     * @throws NullPointerException if {@code criterionText} is null
     */
    public static String normalize(String criterionText) {
        Objects.requireNonNull(criterionText, "criterionText");
        String s = criterionText.strip();
        s = s.replaceAll("\\s+", " ");
        s = s.toLowerCase(Locale.ROOT);
        s = s.replaceAll(TRAILING_STRIP, "");
        return s;
    }

    /**
     * The SHA-256 hex digest (lowercase) of the {@linkplain #normalize normalized} criterion text.
     *
     * @throws NullPointerException if {@code criterionText} is null
     */
    public static String of(String criterionText) {
        String normalized = normalize(criterionText);
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a required algorithm on every JVM; this cannot happen.
            throw new IllegalStateException("SHA-256 not available", e);
        }
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }
}
