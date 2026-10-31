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

import com.rreganjr.requel.annotation.IssueSeverity;
import com.rreganjr.requel.gateway.QueryDescriptions;
import com.rreganjr.requel.gateway.QueryGateway;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * {@code requel context <project> [--min-severity=LOW|MEDIUM|HIGH]} — the composite project-context bundle (summary, tree, glossary,
 * open issues), the same bundle an agent would prime on. Text output shows it as formatted JSON since
 * it is a composite; use {@code --output json} for the raw bundle. {@code --min-severity} narrows the
 * open issues only (#338).
 */
@Command(name = "context", description = QueryDescriptions.GET_PROJECT_CONTEXT)
public class ContextCommand extends AbstractQueryCommand {

    @Parameters(index = "0", paramLabel = "PROJECT", description = QueryDescriptions.PROJECT_NAME)
    String projectName;

    /** #338: list only open issues at or above this severity; picocli refuses anything else. */
    @Option(names = "--min-severity", paramLabel = "LOW|MEDIUM|HIGH",
            description = QueryDescriptions.MIN_SEVERITY)
    IssueSeverity minSeverity;

    @Override
    protected Object query(QueryGateway gateway) {
        return gateway.getProjectContext(projectName, minSeverity);
    }

    @Override
    protected String renderText(Object result) {
        return json(result);
    }
}
