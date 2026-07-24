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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

/**
 * Canonical-normalization and SHA-256 vector tests for {@link CriterionHash} (issue #71). The
 * concrete digest pins the exact algorithm so this ticket and the ticket-4 matcher agree.
 */
class CriterionHashTest {

    // Independently computed: sha256("the system shall allow login").
    private static final String LOGIN_HASH =
            "d5fbdd1a9886a8098c9dc2b75c4fb7c8f33a68ec666df3ad0a33deb795599a05";

    @Test
    void normalizeTrimsCollapsesLowercasesAndStripsTrailingPunctuation() {
        assertThat(CriterionHash.normalize("  The System   SHALL allow Login.  "))
                .isEqualTo("the system shall allow login");
    }

    @Test
    void normalizeIsIdempotentAcrossCaseWhitespaceAndTrailingPunctuation() {
        String a = CriterionHash.normalize("The System SHALL allow Login.");
        String b = CriterionHash.normalize("the   system shall allow login");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void hashMatchesKnownVector() {
        assertThat(CriterionHash.of("  The System   SHALL allow Login.  "))
                .isEqualTo(LOGIN_HASH);
        // Trivial-variant inputs collapse to the same key.
        assertThat(CriterionHash.of("the system shall allow login")).isEqualTo(LOGIN_HASH);
    }

    @Test
    void hashIsLowercase64CharHex() {
        String h = CriterionHash.of("User can reset password!");
        assertThat(h).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void differentTextProducesDifferentHash() {
        assertThat(CriterionHash.of("User can reset password"))
                .isNotEqualTo(CriterionHash.of("User can change password"));
    }

    @Test
    void normalizeRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> CriterionHash.normalize(null));
    }
}
