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

import org.junit.jupiter.api.Test;

class PkceTest {

    @Test
    void s256MatchesRfc7636AppendixBVector() {
        // RFC 7636 Appendix B worked example.
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        assertThat(Pkce.challengeFor(verifier))
                .isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
    }

    @Test
    void generateProducesConsistentUrlSafePair() {
        Pkce pkce = Pkce.generate();
        assertThat(pkce.verifier()).hasSizeGreaterThanOrEqualTo(43)  // >= 256 bits base64url
                .doesNotContain("+", "/", "=");
        assertThat(pkce.challenge()).isEqualTo(Pkce.challengeFor(pkce.verifier()))
                .doesNotContain("+", "/", "=");
    }
}
