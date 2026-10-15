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
import java.util.Objects;

import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.project.CriterionHash;
import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.EditGoalInput;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.EntitySourceLinkDto;
import com.rreganjr.requel.service.api.dto.ExternalSourceDto;
import com.rreganjr.requel.service.api.dto.GoalDto;
import com.rreganjr.requel.service.api.dto.SourceEntitiesDto;
import com.rreganjr.requel.service.api.dto.UpsertFromSourceResultDto;

/**
 * An in-memory stand-in for the gateways {@link RequirementGoalUpserter} talks to (issue #272):
 * goals with EditGoal's name uniqueness, and a simplified UpsertFromSource that resolves by
 * (system, externalId, fragment) and decides CREATED / UNCHANGED / UPDATED / AMBIGUOUS. The real
 * decision table, conflicts included, is covered by {@code EntityProvenanceIT}.
 */
class InMemoryGateway implements CommandGateway, QueryGateway {

    private record GoalRow(long id, int version, String name, String text) {
    }

    private record LinkRow(String system, String externalId, String fragment, long goalId,
            String fragmentHash) {
    }

    private final Map<Long, GoalRow> goals = new LinkedHashMap<>();
    private final List<LinkRow> links = new ArrayList<>();
    private long nextGoalId = 1;
    Map<String, Object> lastUpsert;
    String lastClient;

    int goalCount() {
        return goals.size();
    }

    String goalText(long goalId) {
        return goals.get(goalId).text();
    }

    /** Link an existing goal to a fragment, as LinkSource would. */
    void link(String system, String externalId, String fragment, long goalId, String text) {
        links.add(new LinkRow(system, externalId, fragment, goalId, CriterionHash.of(text)));
    }

    @Override
    public GatewayResult execute(GatewayRequest request) throws GatewayException {
        return switch (request.commandType()) {
            case "EditGoal" -> new GatewayResult("EditGoal", editGoal((EditGoalInput) request.input()));
            case "UpsertFromSource" -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> input = (Map<String, Object>) request.input();
                lastUpsert = input;
                lastClient = request.clientId();
                yield new GatewayResult("UpsertFromSource", upsert(input));
            }
            default -> throw new GatewayException(GatewayException.Kind.NOT_FOUND,
                    "unknown command " + request.commandType());
        };
    }

    @SuppressWarnings("unchecked")
    private UpsertFromSourceResultDto upsert(Map<String, Object> i) throws GatewayException {
        String system = (String) i.get("system");
        String externalId = (String) i.get("externalId");
        String fragment = (String) i.get("fragment");
        String hash = CriterionHash.of((String) i.get("fragmentText"));
        Map<String, Object> goal = (Map<String, Object>) i.get("input");
        Long entityId = (Long) i.get("entityId");
        List<LinkRow> matches = links.stream()
                .filter(l -> l.system().equals(system) && l.externalId().equals(externalId)
                        && Objects.equals(l.fragment(), fragment))
                .filter(l -> entityId == null || l.goalId() == entityId)
                .toList();
        if (matches.size() > 1) {
            return result("AMBIGUOUS", null, matches.stream().map(LinkRow::goalId).toList());
        }
        if (matches.isEmpty()) {
            GoalDto created = editGoal(new EditGoalInput((String) i.get("projectName"), null,
                    (String) goal.get("name"), (String) goal.get("text"), null));
            links.add(new LinkRow(system, externalId, fragment, created.id(), hash));
            return result("CREATED", goals.get(created.id()), List.of());
        }
        LinkRow link = matches.get(0);
        if (link.fragmentHash().equals(hash)) {
            return result("UNCHANGED", goals.get(link.goalId()), List.of());
        }
        editGoal(new EditGoalInput((String) i.get("projectName"), link.goalId(),
                (String) goal.get("name"), (String) goal.get("text"), null));
        links.remove(link);
        links.add(new LinkRow(system, externalId, fragment, link.goalId(), hash));
        return result("UPDATED", goals.get(link.goalId()), List.of());
    }

    private static UpsertFromSourceResultDto result(String status, GoalRow goal,
            List<Long> candidates) {
        return new UpsertFromSourceResultDto(status, "Goal", goal == null ? null : goal.id(),
                goal == null ? null : goal.name(), null, false, null, candidates, null);
    }

    private GoalDto editGoal(EditGoalInput i) throws GatewayException {
        if (i.goalId() == null) {
            // Create: mirror EditGoalCommandImpl's uniqueness conflict on duplicate name.
            boolean nameTaken = goals.values().stream()
                    .anyMatch(g -> g.name().equalsIgnoreCase(i.name()));
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

    @Override
    public SourceEntitiesDto findEntitiesBySource(String projectName, String system,
            String externalId, String fragment) {
        List<EntitySourceLinkDto> found = links.stream()
                .filter(l -> l.system().equals(system) && l.externalId().equals(externalId)
                        && (fragment == null || fragment.equals(l.fragment())))
                .map(l -> new EntitySourceLinkDto(null, null, "DERIVED_FROM", "Goal", l.goalId(),
                        goals.get(l.goalId()).name(), l.fragment(), null, false, false))
                .toList();
        return found.isEmpty() ? null
                : new SourceEntitiesDto(new ExternalSourceDto(null, system, externalId, null, null,
                        null, null, null, null, null), found);
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
        return new AnnotationsDto(List.of(), List.of());
    }

    private static GoalDto goalDto(GoalRow g) {
        return new GoalDto(g.id(), g.version(), g.name(), g.text(), "tester", null, null, null);
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
