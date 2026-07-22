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

import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import java.util.List;
import java.util.Map;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * {@code requel entity <project> <type> <id> [--neighbors]} — read one entity by its domain-interface
 * simple name (e.g. Goal, Story, Actor, UseCase, Scenario, GlossaryTerm) and id, or its related
 * entities grouped by relationship with {@code --neighbors}.
 */
@Command(name = "entity", description = "Read one project entity by type + id (or its --neighbors).")
public class EntityCommand extends AbstractQueryCommand {

    @Parameters(index = "0", paramLabel = "PROJECT", description = "Project name.")
    String projectName;

    @Parameters(index = "1", paramLabel = "TYPE", description = "Entity type, e.g. Goal, Story, Actor.")
    String entityType;

    @Parameters(index = "2", paramLabel = "ID", description = "Entity id.")
    long entityId;

    @Option(names = "--neighbors", description = "Show related entities grouped by relationship.")
    boolean neighbors;

    @Override
    protected Object query(QueryGateway gateway) {
        return neighbors
                ? gateway.getEntityNeighbors(projectName, entityType, entityId)
                : gateway.getEntity(projectName, entityType, entityId);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String renderText(Object result) {
        if (!neighbors) {
            // A single entity detail DTO — no fixed shape here, so show it as JSON.
            return json(result);
        }
        Map<String, List<EntityReferenceDto>> byRelation = (Map<String, List<EntityReferenceDto>>) result;
        if (byRelation.isEmpty()) {
            return "No related entities.";
        }
        StringBuilder sb = new StringBuilder();
        byRelation.forEach((relation, refs) -> {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(relation).append(':');
            for (EntityReferenceDto r : refs) {
                sb.append("\n  ").append(r.entityType()).append(" #").append(r.id())
                        .append("  ").append(r.name());
            }
        });
        return sb.toString();
    }
}
