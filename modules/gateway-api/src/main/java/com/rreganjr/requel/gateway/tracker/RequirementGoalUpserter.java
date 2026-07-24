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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.gateway.provenance.CriterionHash;
import com.rreganjr.requel.gateway.provenance.GoalNameDerivation;
import com.rreganjr.requel.gateway.provenance.ProvenanceDescriptor;
import com.rreganjr.requel.gateway.provenance.ProvenanceNotes;
import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.EditGoalInput;
import com.rreganjr.requel.service.api.dto.EditNoteInput;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.GoalDto;
import com.rreganjr.requel.service.api.dto.NoteDto;

/**
 * Implements the convenience {@code requel.upsertGoalFromRequirement} capability (issue #71):
 * turn one discrete requirement into a provenance-noted goal, resolving an existing goal id first
 * so a re-run <em>updates</em> rather than duplicates.
 *
 * <p>This is deliberately front-end-agnostic: it composes the existing {@link CommandGateway}
 * (writes) and {@link QueryGateway} (reads) plus the shared provenance utilities, so the MCP
 * tools, the CLI, and the remote connector can all reuse it. It adds no new write path — every
 * mutation still goes through {@code EditGoal}/{@code EditNote} and the normal
 * authorization/audit/SSE chain.</p>
 *
 * <h2>Resolution (v1)</h2>
 * <ol>
 *   <li><b>Provenance match:</b> scan the project's goals' notes for a {@code requel-provenance}
 *       block whose {@code sourceSystem} + {@code sourceRef} + {@code criterionHash} equal this
 *       request's. A match is the same requirement on a re-run → update that goal by id and
 *       update its provenance note by id.</li>
 *   <li><b>No match → create:</b> create a new goal. If the derived name already belongs to some
 *       other goal, apply the collision rule (append a short {@code criterionHash} disambiguator)
 *       so the create never trips the {@code EditGoal} uniqueness conflict, then attach a fresh
 *       provenance note.</li>
 * </ol>
 *
 * <p><b>Accepted v1 limitation:</b> a minor edit to a requirement changes both the derived name
 * and the hash, so it neither matches provenance nor collides by name — it creates a new goal and
 * leaves the prior one as a discoverable orphan (same {@code sourceRef}, different hash). Fuzzy
 * resolution is ticket 4.</p>
 *
 * <p><b>Cost:</b> the provenance scan reads the notes of each goal in the project (O(#goals) read
 * calls) per upsert. This is correctness-first for v1; ticket 4 adds a candidate query to narrow
 * it.</p>
 */
public class RequirementGoalUpserter {

    private static final String GOAL_TYPE = "Goal";
    /** Upper bound on goal body text accepted at the tool boundary (client text is untrusted). */
    static final int MAX_TEXT_LENGTH = 20_000;

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    public RequirementGoalUpserter(CommandGateway commandGateway, QueryGateway queryGateway) {
        this.commandGateway = Objects.requireNonNull(commandGateway, "commandGateway");
        this.queryGateway = Objects.requireNonNull(queryGateway, "queryGateway");
    }

    /**
     * Create or update the goal for {@code request}'s requirement and (re)attach its provenance
     * note.
     *
     * @throws GatewayException if an underlying command is rejected, unauthorized, or fails
     */
    public UpsertGoalResult upsert(UpsertGoalRequest request) throws GatewayException {
        Objects.requireNonNull(request, "request");

        String project = request.projectName();
        String hash = request.criterionHash() != null && !request.criterionHash().isBlank()
                ? request.criterionHash()
                : CriterionHash.of(request.criterionText());
        String derivedName = request.name() != null && !request.name().isBlank()
                ? capName(request.name())
                : GoalNameDerivation.deriveName(request.criterionText());
        String text = capText(request.text() != null ? request.text() : request.criterionText());

        List<EntityReferenceDto> goals = goalRefs(project);
        Match match = findProvenanceMatch(project, goals, request.sourceSystem(),
                request.sourceRef(), hash);

        ProvenanceDescriptor provenance = new ProvenanceDescriptor(
                ProvenanceDescriptor.CURRENT_VERSION, request.client(), request.sourceSystem(),
                request.sourceRef(), request.sourceUrl(), request.criterionRef(), hash);
        String noteText = ProvenanceNotes.render(provenance);

        long goalId;
        String finalName;
        boolean created;
        Long existingNoteId;
        if (match != null) {
            // Same requirement, re-run: update the goal in place and refresh its provenance note.
            GoalDto goal = editGoal(project, match.goalId(), derivedName, text, request.client());
            goalId = goal.id();
            finalName = goal.name();
            existingNoteId = match.noteId();
            created = false;
        } else {
            // New requirement: create, disambiguating the name only if it collides with an
            // unrelated goal so the create cannot hit the uniqueness conflict.
            finalName = nameContained(goals, derivedName)
                    ? GoalNameDerivation.disambiguate(derivedName, hash)
                    : derivedName;
            GoalDto goal = editGoal(project, null, finalName, text, request.client());
            goalId = goal.id();
            finalName = goal.name();
            existingNoteId = null;
            created = true;
        }

        NoteDto note = editNote(project, goalId, existingNoteId, noteText, request.client());
        return new UpsertGoalResult(goalId, finalName, note.id(), created, hash);
    }

    private Match findProvenanceMatch(String project, List<EntityReferenceDto> goals,
            String sourceSystem, String sourceRef, String hash) {
        for (EntityReferenceDto ref : goals) {
            AnnotationsDto annotations = queryGateway.getAnnotations(project, GOAL_TYPE, ref.id());
            if (annotations == null || annotations.notes() == null) {
                continue;
            }
            for (NoteDto note : annotations.notes()) {
                Optional<ProvenanceDescriptor> parsed = ProvenanceNotes.parse(note.text());
                if (parsed.isPresent() && matches(parsed.get(), sourceSystem, sourceRef, hash)) {
                    return new Match(ref.id(), note.id());
                }
            }
        }
        return null;
    }

    private static boolean matches(ProvenanceDescriptor p, String sourceSystem, String sourceRef,
            String hash) {
        return Objects.equals(p.sourceSystem(), sourceSystem)
                && Objects.equals(p.sourceRef(), sourceRef)
                && Objects.equals(p.criterionHash(), hash);
    }

    private List<EntityReferenceDto> goalRefs(String project) {
        // Empty query matches every entity; keep only goals.
        return queryGateway.searchProjectEntities(project, "").stream()
                .filter(ref -> GOAL_TYPE.equals(ref.entityType()))
                .toList();
    }

    private static boolean nameContained(List<EntityReferenceDto> goals, String name) {
        return goals.stream().anyMatch(ref -> name.equals(ref.name()));
    }

    private GoalDto editGoal(String project, Long goalId, String name, String text, String client)
            throws GatewayException {
        GatewayResult result = commandGateway.execute(new GatewayRequest("EditGoal",
                new EditGoalInput(project, goalId, name, text, null), client));
        return (GoalDto) result.result();
    }

    private NoteDto editNote(String project, long goalId, Long noteId, String text, String client)
            throws GatewayException {
        GatewayResult result = commandGateway.execute(new GatewayRequest("EditNote",
                new EditNoteInput(project, GOAL_TYPE, goalId, noteId, text), client));
        return (NoteDto) result.result();
    }

    private static String capName(String name) {
        String stripped = name.strip();
        return stripped.length() <= GoalNameDerivation.MAX_NAME_LENGTH
                ? stripped
                : stripped.substring(0, GoalNameDerivation.MAX_NAME_LENGTH).strip();
    }

    private static String capText(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH);
    }

    /** A resolved existing goal and its provenance note. */
    private record Match(long goalId, Long noteId) {
    }
}
