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
import com.rreganjr.requel.gateway.provenance.CriterionHash;
import com.rreganjr.requel.gateway.provenance.GoalNameDerivation;
import com.rreganjr.requel.gateway.provenance.ProvenanceDescriptor;
import com.rreganjr.requel.gateway.provenance.ProvenanceNotes;
import com.rreganjr.requel.service.api.dto.EditGoalInput;

/**
 * Behavioural tests for {@link RequirementGoalUpserter} (issue #71) over the in-memory gateway
 * double, covering the issue's testing strategy: idempotency, the edited-requirement orphan, name
 * collision, provenance-note update-by-id, and the raw create conflict that proves the lookup path
 * is required.
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

    @Test
    void createsGoalWithParseableProvenanceNote() throws Exception {
        UpsertGoalResult result = upserter.upsert(req("The system shall allow login.", "PROJ-1#1"));

        assertThat(result.created()).isTrue();
        assertThat(result.goalId()).isNotNull();
        assertThat(result.noteId()).isNotNull();
        assertThat(result.goalName()).isEqualTo("The system shall allow login");
        assertThat(gateway.goalCount()).isEqualTo(1);
        assertThat(gateway.noteCount()).isEqualTo(1);

        ProvenanceDescriptor prov = provenanceOn(result.goalId());
        assertThat(prov.sourceSystem()).isEqualTo("jira");
        assertThat(prov.sourceRef()).isEqualTo("PROJ-1#1");
        assertThat(prov.criterionHash())
                .isEqualTo(CriterionHash.of("The system shall allow login."));
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
    void reRunUpdatesInPlaceWithNoDuplicateAndSameIds() throws Exception {
        UpsertGoalRequest request = req("The system shall allow login.", "PROJ-1#1");

        UpsertGoalResult first = upserter.upsert(request);
        UpsertGoalResult second = upserter.upsert(request);

        assertThat(second.created()).isFalse();
        assertThat(second.goalId()).isEqualTo(first.goalId());
        assertThat(second.noteId()).isEqualTo(first.noteId()); // provenance note updated, not added
        assertThat(gateway.goalCount()).isEqualTo(1);
        assertThat(gateway.noteCount()).isEqualTo(1);
    }

    @Test
    void editedRequirementCreatesNewGoalAndLeavesPriorAsOrphan() throws Exception {
        UpsertGoalResult original = upserter.upsert(req("The system shall allow login.", "PROJ-1#1"));
        // Same source ref, minor edit to the text -> different derived name and hash.
        UpsertGoalResult edited =
                upserter.upsert(req("The system shall allow secure login.", "PROJ-1#1"));

        assertThat(edited.created()).isTrue();
        assertThat(edited.goalId()).isNotEqualTo(original.goalId());
        assertThat(gateway.goalCount()).isEqualTo(2); // prior goal discoverable as an orphan
    }

    @Test
    void distinctRequirementsSharingADerivedNameAreDisambiguated() throws Exception {
        UpsertGoalResult a = upserter.upsert(req("Allow login. Via SSO.", "PROJ-1#1"));
        UpsertGoalResult b = upserter.upsert(req("Allow login. Via password.", "PROJ-2#1"));

        assertThat(a.goalName()).isEqualTo("Allow login");
        assertThat(b.created()).isTrue();
        assertThat(b.goalName()).isNotEqualTo(a.goalName());
        assertThat(b.goalName()).startsWith("Allow login-");
        assertThat(gateway.goalCount()).isEqualTo(2); // no uniqueness conflict, both persisted
    }

    @Test
    void rawCreateWithCollidingNameSurfacesUniquenessConflict() throws Exception {
        // Proves why the upserter must resolve an id: a bare second create by the same name fails.
        gateway.execute(new GatewayRequest("EditGoal",
                new EditGoalInput("Demo", null, "Allow login", "x", null), null));

        assertThatExceptionOfType(GatewayException.class).isThrownBy(() ->
                gateway.execute(new GatewayRequest("EditGoal",
                        new EditGoalInput("Demo", null, "Allow login", "y", null), null)));
    }

    private ProvenanceDescriptor provenanceOn(Long goalId) {
        return gateway.getAnnotations("Demo", "Goal", goalId).notes().stream()
                .map(n -> ProvenanceNotes.parse(n.text()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .findFirst()
                .orElseThrow();
    }
}
