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
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/** {@code requel search <project> <query>} — find project entities by name (case-insensitive). */
@Command(name = "search", description = "Search a project's entities by name.")
public class SearchCommand extends AbstractQueryCommand {

    @Parameters(index = "0", paramLabel = "PROJECT", description = "Project name.")
    String projectName;

    @Parameters(index = "1", paramLabel = "QUERY", description = "Name substring to match.")
    String queryText;

    @Override
    protected Object query(QueryGateway gateway) {
        return gateway.searchProjectEntities(projectName, queryText);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String renderText(Object result) {
        List<EntityReferenceDto> refs = (List<EntityReferenceDto>) result;
        if (refs.isEmpty()) {
            return "No matches.";
        }
        StringBuilder sb = new StringBuilder();
        for (EntityReferenceDto r : refs) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(r.entityType()).append(" #").append(r.id()).append("  ").append(r.name());
        }
        return sb.toString();
    }
}
