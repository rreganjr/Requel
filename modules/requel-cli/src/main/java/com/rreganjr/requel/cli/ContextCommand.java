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
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * {@code requel context <project>} — the composite project-context bundle (summary, tree, glossary,
 * open issues), the same bundle an agent would prime on. Text output shows it as formatted JSON since
 * it is a composite; use {@code --output json} for the raw bundle.
 */
@Command(name = "context", description = "Show a project's composite context bundle.")
public class ContextCommand extends AbstractQueryCommand {

    @Parameters(index = "0", paramLabel = "PROJECT", description = "Project name.")
    String projectName;

    @Override
    protected Object query(QueryGateway gateway) {
        return gateway.getProjectContext(projectName);
    }

    @Override
    protected String renderText(Object result) {
        return json(result);
    }
}
