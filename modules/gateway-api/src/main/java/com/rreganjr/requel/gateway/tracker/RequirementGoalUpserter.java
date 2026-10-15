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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.gateway.provenance.GoalNameDerivation;
import com.rreganjr.requel.project.CriterionHash;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.EntitySourceLinkDto;
import com.rreganjr.requel.service.api.dto.SourceEntitiesDto;
import com.rreganjr.requel.service.api.dto.UpsertFromSourceResultDto;

/**
 * Implements the convenience {@code upsertGoalFromRequirement} capability: turn one discrete
 * requirement into a goal built from a fragment of a tracker item, so a re-run updates rather
 * than duplicates.
 *
 * <p>Since #272 it is a thin, goal-specific wrapper over the {@code UpsertFromSource} command,
 * which records the source, resolves the goal by (source, fragment), and refuses to overwrite a
 * goal edited in Requel since the last ingest (it raises a conflict issue instead). What this
 * class adds is the goal-specific part #71 defined: the goal name derived from the criterion, the
 * collision rule for a derived name another goal already has, and the default fragment.</p>
 *
 * <h2>Fragment</h2>
 * The fragment is {@code criterionRef} when the caller gives one. Without it, it is
 * {@code hash:<first 12 of criterionHash>} — #71's behaviour, where the criterion's text was its
 * identity, kept so existing callers keep resolving to the goals they made (#272 P4). The same
 * default is what V26 used to convert #71's provenance notes. With a {@code criterionRef}, an
 * edited criterion updates its goal in place; without one it cannot, because its identity changed.
 *
 * <h2>Name collision</h2>
 * If the derived name already belongs to a goal that did not come from this fragment, a short
 * {@code criterionHash} disambiguator is appended ({@link GoalNameDerivation#disambiguate}), so
 * the create cannot hit the goal-name uniqueness conflict.
 */
public class RequirementGoalUpserter {

    private static final String GOAL_TYPE = "Goal";
    /** Upper bound on goal body text accepted at the tool boundary (client text is untrusted). */
    static final int MAX_TEXT_LENGTH = 20_000;
    /** Length of the criterion-hash prefix used as the default fragment. */
    static final int HASH_FRAGMENT_LENGTH = 12;

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    public RequirementGoalUpserter(CommandGateway commandGateway, QueryGateway queryGateway) {
        this.commandGateway = Objects.requireNonNull(commandGateway, "commandGateway");
        this.queryGateway = Objects.requireNonNull(queryGateway, "queryGateway");
    }

    /**
     * Create or update the goal for {@code request}'s requirement.
     *
     * @throws GatewayException if the upsert is rejected, unauthorized, or fails
     */
    public UpsertGoalResult upsert(UpsertGoalRequest request) throws GatewayException {
        Objects.requireNonNull(request, "request");

        String project = request.projectName();
        String hash = CriterionHash.of(request.criterionText());
        String fragment = defaultFragment(request.criterionRef(), hash);
        String derivedName = request.name() != null && !request.name().isBlank()
                ? capName(request.name())
                : GoalNameDerivation.deriveName(request.criterionText());
        String text = capText(request.text() != null ? request.text() : request.criterionText());

        // A derived name another goal already has is disambiguated, unless that goal is the one
        // this fragment already produced (then it is simply being updated).
        Set<Long> fromFragment = goalsFromFragment(project, request.sourceSystem(),
                request.sourceRef(), fragment);
        String name = nameTakenByAnotherGoal(project, derivedName, fromFragment)
                ? GoalNameDerivation.disambiguate(derivedName, hash)
                : derivedName;

        Map<String, Object> goal = new HashMap<>();
        goal.put("name", name);
        goal.put("text", text);
        Map<String, Object> upsert = new HashMap<>();
        upsert.put("projectName", project);
        upsert.put("command", "EditGoal");
        upsert.put("input", goal);
        upsert.put("system", request.sourceSystem());
        upsert.put("externalId", request.sourceRef());
        if (request.sourceUrl() != null && !request.sourceUrl().isBlank()) {
            upsert.put("locatorType", "URL");
            upsert.put("locator", request.sourceUrl());
        }
        upsert.put("sourceVersion", request.sourceVersion());
        upsert.put("fragment", fragment);
        upsert.put("fragmentText", request.criterionText());
        upsert.put("entityId", request.goalId());

        UpsertFromSourceResultDto result = (UpsertFromSourceResultDto) commandGateway.execute(
                new GatewayRequest("UpsertFromSource", upsert, request.client())).result();
        return new UpsertGoalResult(result.entityId(), result.entityName(),
                "CREATED".equals(result.status()), hash, result.status(), fragment,
                result.issueId(), result.candidates());
    }

    /** {@code criterionRef}, or {@code hash:<12>} when there is none (#272 P4). */
    static String defaultFragment(String criterionRef, String criterionHash) {
        if (criterionRef != null && !criterionRef.isBlank()) {
            return criterionRef.strip();
        }
        return "hash:" + criterionHash.substring(0, HASH_FRAGMENT_LENGTH);
    }

    private Set<Long> goalsFromFragment(String project, String system, String externalId,
            String fragment) {
        SourceEntitiesDto produced = queryGateway.findEntitiesBySource(project, system, externalId,
                fragment);
        if (produced == null || produced.links() == null) {
            return Set.of();
        }
        return produced.links().stream()
                .filter(link -> GOAL_TYPE.equals(link.entityType()))
                .map(EntitySourceLinkDto::entityId)
                .collect(Collectors.toSet());
    }

    private boolean nameTakenByAnotherGoal(String project, String name, Set<Long> fromFragment) {
        List<EntityReferenceDto> goals = queryGateway.searchProjectEntities(project, name).stream()
                .filter(ref -> GOAL_TYPE.equals(ref.entityType()))
                .toList();
        return goals.stream().anyMatch(ref -> name.equalsIgnoreCase(ref.name())
                && !fromFragment.contains(ref.id()));
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
}
