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

import com.rreganjr.requel.gateway.QueryDescriptions;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.GoalTraceDto;
import com.rreganjr.requel.service.api.dto.TraceGapDto;
import com.rreganjr.requel.service.api.dto.TraceabilityDto;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * {@code requel traceability <project>} (#497): what references each goal, then the gaps grouped
 * by kind.
 */
@Command(name = "traceability", description = QueryDescriptions.GET_TRACEABILITY)
public class TraceabilityCommand extends AbstractQueryCommand {

    /** How each gap kind reads as a heading. */
    static final Map<String, String> KIND_LABELS = Map.of(
            TraceGapDto.GOAL_NOT_REALIZED, "Goals no story or use case realizes",
            TraceGapDto.ACTOR_NOT_USED, "Actors nothing uses",
            TraceGapDto.STORY_WITHOUT_GOAL, "Stories with no goal",
            TraceGapDto.STORY_NOT_IN_USE_CASE, "Stories in no use case",
            TraceGapDto.USE_CASE_WITHOUT_GOAL, "Use cases with no goal",
            TraceGapDto.SCENARIO_NOT_IN_USE_CASE, "Scenarios no use case reaches",
            TraceGapDto.STAKEHOLDER_WITHOUT_GOAL, "Stakeholders with no goal");

    @Parameters(index = "0", paramLabel = "PROJECT", description = QueryDescriptions.PROJECT_NAME)
    String projectName;

    @Override
    protected Object query(QueryGateway gateway) {
        return gateway.getTraceability(projectName);
    }

    @Override
    protected String renderText(Object result) {
        TraceabilityDto trace = (TraceabilityDto) result;
        StringBuilder sb = new StringBuilder("Goals");
        if (trace.goals().isEmpty()) {
            sb.append("\n  (none)");
        }
        for (GoalTraceDto goal : trace.goals()) {
            sb.append("\n  Goal #").append(goal.id()).append(' ').append(goal.name());
            if (!goal.realized()) {
                sb.append("  (not realized)");
            }
            appendRefs(sb, "stakeholders", goal.stakeholders());
            appendRefs(sb, "actors", goal.actors());
            appendRefs(sb, "stories", goal.stories());
            appendRefs(sb, "use cases", goal.useCases());
        }
        sb.append("\n\nGaps");
        if (trace.gaps().isEmpty()) {
            sb.append("\n  None: everything traces.");
            return sb.toString();
        }
        for (String kind : TraceGapDto.KINDS) {
            List<EntityReferenceDto> entities = trace.gaps().stream()
                    .filter(g -> kind.equals(g.kind())).map(TraceGapDto::entity).toList();
            if (entities.isEmpty()) {
                continue;
            }
            sb.append("\n  ").append(KIND_LABELS.get(kind)).append(" (").append(entities.size()).append(')');
            for (EntityReferenceDto e : entities) {
                sb.append("\n    ").append(e.entityType()).append(" #").append(e.id()).append(' ').append(e.name());
            }
        }
        return sb.toString();
    }

    private static void appendRefs(StringBuilder sb, String label, List<EntityReferenceDto> refs) {
        if (!refs.isEmpty()) {
            sb.append("\n    ").append(label).append(": ")
                    .append(refs.stream().map(EntityReferenceDto::name).collect(Collectors.joining(", ")));
        }
    }
}
