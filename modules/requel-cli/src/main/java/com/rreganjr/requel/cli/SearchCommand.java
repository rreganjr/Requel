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
import com.rreganjr.requel.service.api.dto.ProjectSearchResultDto;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * {@code requel search <project> <query> [--type T] [--offset N] [--limit N]} — search a
 * project's requirements by name and text, and its issues by text (#504).
 */
@Command(name = "search", description = QueryDescriptions.SEARCH_PROJECT_ENTITIES)
public class SearchCommand extends AbstractQueryCommand {

    @Parameters(index = "0", paramLabel = "PROJECT", description = QueryDescriptions.PROJECT_NAME)
    String projectName;

    @Parameters(index = "1", paramLabel = "QUERY", description = QueryDescriptions.SEARCH_QUERY)
    String queryText;

    @Option(names = "--type", paramLabel = "TYPE", description = QueryDescriptions.SEARCH_ENTITY_TYPE)
    String entityType;

    @Option(names = "--offset", paramLabel = "N", description = QueryDescriptions.SEARCH_OFFSET)
    Integer offset;

    @Option(names = "--limit", paramLabel = "N", description = QueryDescriptions.SEARCH_LIMIT)
    Integer limit;

    @Override
    protected Object query(QueryGateway gateway) {
        return gateway.searchProjectEntities(projectName, queryText, entityType, offset, limit);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String renderText(Object result) {
        List<ProjectSearchResultDto> results = (List<ProjectSearchResultDto>) result;
        if (results.isEmpty()) {
            return "No matches.";
        }
        StringBuilder sb = new StringBuilder();
        for (ProjectSearchResultDto r : results) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(r.entityType()).append(" #").append(r.id()).append("  ").append(r.name());
            if (r.onEntityType() != null) {
                sb.append("  (on ").append(r.onEntityType()).append(" #").append(r.onEntityId())
                        .append(')');
            }
            sb.append("\n    ").append(r.matchedField()).append(": ").append(r.snippet());
        }
        return sb.toString();
    }
}
