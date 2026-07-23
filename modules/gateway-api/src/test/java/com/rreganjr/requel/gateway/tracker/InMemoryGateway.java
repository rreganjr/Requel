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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.EditGoalInput;
import com.rreganjr.requel.service.api.dto.EditNoteInput;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.GoalDto;
import com.rreganjr.requel.service.api.dto.NoteDto;

/**
 * In-memory {@link CommandGateway} + {@link QueryGateway} double for {@link RequirementGoalUpserter}
 * tests. It models just enough of Requel's goal/note behavior — crucially the
 * <b>{@code EditGoal} uniqueness conflict on create by name</b> — to exercise the upserter's
 * resolution logic without Spring or JPA.
 */
class InMemoryGateway implements CommandGateway, QueryGateway {

    private record GoalRow(long id, int version, String name, String text) {
    }

    private record NoteRow(long id, int version, String text, long goalId) {
    }

    private final Map<Long, GoalRow> goals = new LinkedHashMap<>();
    private final Map<Long, NoteRow> notes = new LinkedHashMap<>();
    private long nextGoalId = 1;
    private long nextNoteId = 1;

    int goalCount() {
        return goals.size();
    }

    int noteCount() {
        return notes.size();
    }

    @Override
    public GatewayResult execute(GatewayRequest request) throws GatewayException {
        return switch (request.commandType()) {
            case "EditGoal" -> new GatewayResult("EditGoal", editGoal((EditGoalInput) request.input()));
            case "EditNote" -> new GatewayResult("EditNote", editNote((EditNoteInput) request.input()));
            default -> throw new GatewayException(GatewayException.Kind.NOT_FOUND,
                    "unknown command " + request.commandType());
        };
    }

    private GoalDto editGoal(EditGoalInput i) throws GatewayException {
        if (i.goalId() == null) {
            // Create: mirror EditGoalCommandImpl's uniqueness conflict on duplicate name.
            boolean nameTaken = goals.values().stream().anyMatch(g -> g.name().equals(i.name()));
            if (nameTaken) {
                throw new GatewayException(GatewayException.Kind.EXECUTION_ERROR,
                        "a goal named '" + i.name() + "' already exists");
            }
            long id = nextGoalId++;
            goals.put(id, new GoalRow(id, 0, i.name(), i.text()));
            return goalDto(goals.get(id));
        }
        GoalRow existing = goals.get(i.goalId());
        if (existing == null) {
            throw new GatewayException(GatewayException.Kind.NOT_FOUND, "no goal " + i.goalId());
        }
        GoalRow updated = new GoalRow(existing.id(), existing.version() + 1, i.name(), i.text());
        goals.put(updated.id(), updated);
        return goalDto(updated);
    }

    private NoteDto editNote(EditNoteInput i) throws GatewayException {
        if (i.noteId() == null) {
            long id = nextNoteId++;
            notes.put(id, new NoteRow(id, 0, i.text(), i.entityId()));
            return noteDto(notes.get(id));
        }
        NoteRow existing = notes.get(i.noteId());
        if (existing == null) {
            throw new GatewayException(GatewayException.Kind.NOT_FOUND, "no note " + i.noteId());
        }
        NoteRow updated = new NoteRow(existing.id(), existing.version() + 1, i.text(),
                existing.goalId());
        notes.put(updated.id(), updated);
        return noteDto(updated);
    }

    @Override
    public List<EntityReferenceDto> searchProjectEntities(String projectName, String query) {
        String needle = query == null ? "" : query.toLowerCase(Locale.ROOT);
        List<EntityReferenceDto> out = new ArrayList<>();
        for (GoalRow g : goals.values()) {
            if (g.name().toLowerCase(Locale.ROOT).contains(needle)) {
                out.add(new EntityReferenceDto("Goal", g.id(), g.name()));
            }
        }
        return out;
    }

    @Override
    public AnnotationsDto getAnnotations(String projectName, String entityType, long entityId) {
        List<NoteDto> goalNotes = notes.values().stream()
                .filter(n -> n.goalId() == entityId)
                .map(InMemoryGateway::noteDto)
                .toList();
        return new AnnotationsDto(goalNotes, List.of());
    }

    private static GoalDto goalDto(GoalRow g) {
        return new GoalDto(g.id(), g.version(), g.name(), g.text(), "tester", null, null, null);
    }

    private static NoteDto noteDto(NoteRow n) {
        return new NoteDto(n.id(), n.version(), n.text(), "tester");
    }

    // --- unused QueryGateway surface -------------------------------------------------------

    @Override
    public List<com.rreganjr.requel.service.api.dto.ProjectDto> listProjects() {
        throw new UnsupportedOperationException();
    }

    @Override
    public com.rreganjr.requel.service.api.dto.ProjectDto getProject(String projectName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto> getProjectTree(
            String projectName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<com.rreganjr.requel.service.api.dto.GlossaryTermDto> getGlossaryTerms(
            String projectName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<com.rreganjr.requel.service.api.dto.OpenIssueDto> getOpenIssues(String projectName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getEntity(String projectName, String entityType, long entityId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, List<EntityReferenceDto>> getEntityNeighbors(String projectName,
            String entityType, long entityId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getProjectContext(String projectName) {
        throw new UnsupportedOperationException();
    }
}
