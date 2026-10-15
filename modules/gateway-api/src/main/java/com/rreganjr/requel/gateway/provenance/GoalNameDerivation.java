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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives a deterministic goal name from a requirement statement (issue #71).
 *
 * <p>Derivation: take the leading clause — the text up to the first line break, then up to the
 * first sentence terminator ({@code . ! ?} followed by whitespace or end) — collapse whitespace,
 * strip trailing sentence punctuation, and cap at {@link #MAX_NAME_LENGTH} characters on a word
 * boundary where possible.</p>
 *
 * <p><b>Collision rule.</b> Two requirements can derive the same name. When the caller detects a
 * collision within the target project it appends a short, stable disambiguator via
 * {@link #disambiguate(String, String)}: {@code "-" + } the first six characters of the
 * requirement's {@code criterionHash}, truncating the base name if needed to stay within the
 * length cap.</p>
 *
 * <p>Stateless and thread-safe.</p>
 */
public final class GoalNameDerivation {

    /** Maximum derived goal-name length. */
    public static final int MAX_NAME_LENGTH = 120;

    /** Length of the criterionHash prefix used as a collision disambiguator. */
    public static final int DISAMBIGUATOR_LEN = 6;

    private static final Pattern SENTENCE_END = Pattern.compile("[.!?](\\s|$)");
    private static final String TRAILING_STRIP = "[.,;:!?\\s]+$";

    private GoalNameDerivation() {
    }

    /**
     * Derive a goal name from {@code requirement}.
     *
     * @throws IllegalArgumentException if {@code requirement} is null/blank or yields no usable
     *                                  name
     */
    public static String deriveName(String requirement) {
        if (requirement == null || requirement.isBlank()) {
            throw new IllegalArgumentException("requirement text is required to derive a goal name");
        }
        String name = stripTrailing(collapse(firstClause(requirement)));
        if (name.isBlank()) {
            // Leading clause was only punctuation/whitespace; fall back to the whole statement.
            name = stripTrailing(collapse(requirement));
        }
        name = cap(name);
        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "could not derive a goal name from requirement: " + requirement);
        }
        return name;
    }

    /**
     * Append a stable disambiguator derived from {@code criterionHash} to {@code baseName},
     * truncating the base if necessary so the result stays within {@link #MAX_NAME_LENGTH}.
     *
     * @throws IllegalArgumentException if {@code criterionHash} is shorter than
     *                                  {@link #DISAMBIGUATOR_LEN}
     */
    public static String disambiguate(String baseName, String criterionHash) {
        if (criterionHash == null || criterionHash.length() < DISAMBIGUATOR_LEN) {
            throw new IllegalArgumentException(
                    "criterionHash must be at least " + DISAMBIGUATOR_LEN + " characters");
        }
        String suffix = "-" + criterionHash.substring(0, DISAMBIGUATOR_LEN);
        String base = baseName == null ? "" : baseName;
        int room = MAX_NAME_LENGTH - suffix.length();
        if (base.length() > room) {
            base = stripTrailing(base.substring(0, room));
        }
        return base + suffix;
    }

    private static String firstClause(String text) {
        int nl = text.length();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                nl = i;
                break;
            }
        }
        String line = text.substring(0, nl);
        Matcher m = SENTENCE_END.matcher(line);
        if (m.find()) {
            return line.substring(0, m.start());
        }
        return line;
    }

    private static String collapse(String s) {
        return s.strip().replaceAll("\\s+", " ");
    }

    private static String stripTrailing(String s) {
        return s.replaceAll(TRAILING_STRIP, "");
    }

    private static String cap(String s) {
        if (s.length() <= MAX_NAME_LENGTH) {
            return s;
        }
        String cut = s.substring(0, MAX_NAME_LENGTH);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > 0) {
            cut = cut.substring(0, lastSpace);
        }
        return stripTrailing(cut);
    }
}
