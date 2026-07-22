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
import com.rreganjr.requel.service.api.dto.OpenIssueDto;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/** {@code requel open-issues <project>} — unresolved issues across the project's entities. */
@Command(name = "open-issues", description = "List a project's unresolved issues.")
public class OpenIssuesCommand extends AbstractQueryCommand {

    @Parameters(index = "0", paramLabel = "PROJECT", description = "Project name.")
    String projectName;

    @Override
    protected Object query(QueryGateway gateway) {
        return gateway.getOpenIssues(projectName);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String renderText(Object result) {
        List<OpenIssueDto> issues = (List<OpenIssueDto>) result;
        if (issues.isEmpty()) {
            return "No open issues.";
        }
        StringBuilder sb = new StringBuilder();
        for (OpenIssueDto i : issues) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append('[').append(i.entityType()).append(' ').append(i.entityName()).append("] ")
                    .append(i.issueText());
            if (i.mustBeResolved()) {
                sb.append("  (must resolve)");
            }
        }
        return sb.toString();
    }
}
