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
package com.rreganjr.requel.gateway.tracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.provenance.GoalNameDerivation;
import com.rreganjr.requel.project.CriterionHash;
import com.rreganjr.requel.service.api.dto.EditGoalInput;

/**
 * Behavioural tests for {@link RequirementGoalUpserter} over the in-memory gateway double: what it
 * sends to UpsertFromSource (issue #272), the default fragment, name derivation and the collision
 * rule. The server-side decision table, conflicts included, is {@code EntityProvenanceIT}.
 */
class RequirementGoalUpserterTest {

    private InMemoryGateway gateway;
    private RequirementGoalUpserter upserter;

    @BeforeEach
    void setUp() {
        gateway = new InMemoryGateway();
        upserter = new RequirementGoalUpserter(gateway, gateway);
    }

    private static UpsertGoalRequest req(String criterionText, String sourceRef) {
        return UpsertGoalRequest.of("Demo", criterionText, "jira", sourceRef,
                "https://example/browse/" + sourceRef, "AC-1", "claude-desktop");
    }

    private static UpsertGoalRequest reqWithoutRef(String criterionText, String sourceRef) {
        return UpsertGoalRequest.of("Demo", criterionText, "jira", sourceRef, null, null,
                "claude-desktop");
    }

    @Test
    void createsAGoalThroughUpsertFromSourceAndWritesNoNote() throws Exception {
        UpsertGoalResult result = upserter.upsert(req("The system shall allow login.", "PROJ-1"));

        assertThat(result.created()).isTrue();
        assertThat(result.status()).isEqualTo("CREATED");
        assertThat(result.goalId()).isNotNull();
        assertThat(result.goalName()).isEqualTo("The system shall allow login");
        assertThat(result.fragment()).isEqualTo("AC-1");
        assertThat(gateway.goalCount()).isEqualTo(1);

        assertThat(gateway.lastUpsert).containsEntry("command", "EditGoal")
                .containsEntry("system", "jira")
                .containsEntry("externalId", "PROJ-1")
                .containsEntry("locatorType", "URL")
                .containsEntry("locator", "https://example/browse/PROJ-1")
                .containsEntry("fragment", "AC-1")
                .containsEntry("fragmentText", "The system shall allow login.");
        assertThat(gateway.lastClient).isEqualTo("claude-desktop");
    }

    @Test
    void derivesNameAndHashWhenOmitted() throws Exception {
        UpsertGoalResult result = upserter.upsert(req("Users can export reports to CSV", "GH-9"));

        assertThat(result.goalName())
                .isEqualTo(GoalNameDerivation.deriveName("Users can export reports to CSV"));
        assertThat(result.criterionHash())
                .isEqualTo(CriterionHash.of("Users can export reports to CSV"));
    }

    @Test
    void aSourceWithoutAUrlSendsNoLocator() throws Exception {
        upserter.upsert(reqWithoutRef("The system shall allow login.", "PROJ-1"));

        assertThat(gateway.lastUpsert).doesNotContainKeys("locatorType", "locator");
    }

    @Test
    void reRunIsUnchangedWithNoDuplicate() throws Exception {
        UpsertGoalRequest request = req("The system shall allow login.", "PROJ-1");

        UpsertGoalResult first = upserter.upsert(request);
        UpsertGoalResult second = upserter.upsert(request);

        assertThat(second.created()).isFalse();
        assertThat(second.status()).isEqualTo("UNCHANGED");
        assertThat(second.goalId()).isEqualTo(first.goalId());
        assertThat(gateway.goalCount()).isEqualTo(1);
    }

    @Test
    void anEditedCriterionWithACriterionRefUpdatesItsGoalInPlace() throws Exception {
        UpsertGoalResult original = upserter.upsert(req("The system shall allow login.", "PROJ-1"));
        UpsertGoalResult edited =
                upserter.upsert(req("The system shall allow secure login.", "PROJ-1"));

        assertThat(edited.status()).isEqualTo("UPDATED");
        assertThat(edited.goalId()).isEqualTo(original.goalId());
        assertThat(edited.goalName()).isEqualTo("The system shall allow secure login");
        assertThat(gateway.goalCount()).isEqualTo(1); // #71 left an orphan here
    }

    @Test
    void withoutACriterionRefTheFragmentIsTheCriterionHashAsIn71() throws Exception {
        UpsertGoalResult original = upserter.upsert(
                reqWithoutRef("The system shall allow login.", "PROJ-1"));
        UpsertGoalResult edited = upserter.upsert(
                reqWithoutRef("The system shall allow secure login.", "PROJ-1"));

        assertThat(original.fragment())
                .isEqualTo("hash:" + CriterionHash.of("The system shall allow login.")
                        .substring(0, 12));
        assertThat(edited.created()).isTrue(); // the criterion's text is its identity
        assertThat(gateway.goalCount()).isEqualTo(2);
    }

    @Test
    void distinctRequirementsSharingADerivedNameAreDisambiguated() throws Exception {
        UpsertGoalResult a = upserter.upsert(req("Allow login. Via SSO.", "PROJ-1"));
        UpsertGoalResult b = upserter.upsert(req("Allow login. Via password.", "PROJ-2"));

        assertThat(a.goalName()).isEqualTo("Allow login");
        assertThat(b.created()).isTrue();
        assertThat(b.goalName()).startsWith("Allow login-");
        assertThat(gateway.goalCount()).isEqualTo(2); // no uniqueness conflict, both persisted
    }

    @Test
    void theGoalAFragmentProducedKeepsItsNameOnUpdate() throws Exception {
        UpsertGoalResult first = upserter.upsert(req("Allow login. Via SSO.", "PROJ-1"));
        UpsertGoalResult second = upserter.upsert(req("Allow login. Via SSO or password.",
                "PROJ-1"));

        assertThat(second.status()).isEqualTo("UPDATED");
        assertThat(second.goalName()).isEqualTo(first.goalName()); // not disambiguated against itself
    }

    @Test
    void severalGoalsFromOneCriterionAreAmbiguousUntilAGoalIdPicksOne() throws Exception {
        UpsertGoalResult first = upserter.upsert(req("Ownership moves to Conduit.", "PROJ-1"));
        gateway.execute(new GatewayRequest("EditGoal",
                new EditGoalInput("Demo", null, "Ownership record", "x", null), null));
        gateway.link("jira", "PROJ-1", "AC-1", 2L, "Ownership moves to Conduit.");

        UpsertGoalResult ambiguous = upserter.upsert(req("Ownership moves to Conduit now.",
                "PROJ-1"));
        assertThat(ambiguous.status()).isEqualTo("AMBIGUOUS");
        assertThat(ambiguous.candidates()).containsExactlyInAnyOrder(first.goalId(), 2L);

        UpsertGoalRequest pick = new UpsertGoalRequest("Demo", "Ownership moves to Conduit now.",
                null, null, "jira", "PROJ-1", null, "AC-1", "claude-desktop", null, first.goalId(),
                "v2");
        UpsertGoalResult picked = upserter.upsert(pick);
        assertThat(picked.status()).isEqualTo("UPDATED");
        assertThat(picked.goalId()).isEqualTo(first.goalId());
        assertThat(gateway.lastUpsert).containsEntry("entityId", first.goalId())
                .containsEntry("sourceVersion", "v2");
    }

    @Test
    void rawCreateWithCollidingNameSurfacesUniquenessConflict() throws Exception {
        // Why the collision rule exists: a bare second create by the same name fails.
        gateway.execute(new GatewayRequest("EditGoal",
                new EditGoalInput("Demo", null, "Allow login", "x", null), null));

        assertThatExceptionOfType(GatewayException.class).isThrownBy(() ->
                gateway.execute(new GatewayRequest("EditGoal",
                        new EditGoalInput("Demo", null, "Allow login", "y", null), null)));
    }

    @Test
    void theDefaultFragmentPrefersTheCriterionRef() {
        assertThat(RequirementGoalUpserter.defaultFragment(" AC-2 ", "abcdef0123456789"))
                .isEqualTo("AC-2");
        assertThat(RequirementGoalUpserter.defaultFragment(null, "abcdef0123456789"))
                .isEqualTo("hash:abcdef012345");
    }
}
