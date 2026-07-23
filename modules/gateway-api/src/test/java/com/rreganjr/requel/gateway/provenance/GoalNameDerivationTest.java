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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Deterministic goal-name derivation and collision-disambiguator tests (issue #71).
 */
class GoalNameDerivationTest {

    @Test
    void takesLeadingSentenceAndStripsTerminatingPunctuation() {
        assertThat(GoalNameDerivation.deriveName(
                "The system shall allow login. It must respond within 2 seconds."))
                .isEqualTo("The system shall allow login");
    }

    @Test
    void stopsAtFirstLineBreak() {
        assertThat(GoalNameDerivation.deriveName("Allow password reset\nvia email link"))
                .isEqualTo("Allow password reset");
    }

    @Test
    void collapsesInternalWhitespace() {
        assertThat(GoalNameDerivation.deriveName("  Support   \t multiple  \n sessions "))
                .isEqualTo("Support multiple");
    }

    @Test
    void preservesCaseOfTheRequirement() {
        assertThat(GoalNameDerivation.deriveName("MFA is required for admin users"))
                .isEqualTo("MFA is required for admin users");
    }

    @Test
    void capsAtMaxLengthOnAWordBoundary() {
        String longClause = "The system shall support "
                + "extremely detailed configuration options ".repeat(10);

        String name = GoalNameDerivation.deriveName(longClause);

        assertThat(name.length()).isLessThanOrEqualTo(GoalNameDerivation.MAX_NAME_LENGTH);
        assertThat(name).doesNotEndWith(" ");
        assertThat(longClause).startsWith(name); // prefix, not mangled
    }

    @Test
    void blankOrNullRequirementIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> GoalNameDerivation.deriveName("   "));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> GoalNameDerivation.deriveName(null));
    }

    @Test
    void disambiguateAppendsHashPrefix() {
        assertThat(GoalNameDerivation.disambiguate("Allow login", "abcdef1234567890"))
                .isEqualTo("Allow login-abcdef");
    }

    @Test
    void disambiguateStaysWithinMaxLength() {
        String base = "x".repeat(GoalNameDerivation.MAX_NAME_LENGTH);

        String result = GoalNameDerivation.disambiguate(base, "abcdef1234567890");

        assertThat(result.length()).isLessThanOrEqualTo(GoalNameDerivation.MAX_NAME_LENGTH);
        assertThat(result).endsWith("-abcdef");
    }

    @Test
    void disambiguateRejectsShortHash() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> GoalNameDerivation.disambiguate("Allow login", "abc"));
    }

    @Test
    void twoRequirementsCanDeriveTheSameBaseNameThenDivergeWithDisambiguator() {
        String a = GoalNameDerivation.deriveName("Allow login. Via SSO.");
        String b = GoalNameDerivation.deriveName("Allow login. Via password.");
        assertThat(a).isEqualTo(b); // collision

        String da = GoalNameDerivation.disambiguate(a, CriterionHash.of("Allow login via SSO"));
        String db = GoalNameDerivation.disambiguate(b, CriterionHash.of("Allow login via password"));
        assertThat(da).isNotEqualTo(db); // disambiguated
    }
}
