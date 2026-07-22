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
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** {@code requel project <name> [--tree]} — a project's summary, or its content tree. */
@Command(name = "project", description = "Show a project's summary, or its content tree with --tree.")
public class ProjectCommand extends AbstractQueryCommand {

    @Parameters(index = "0", paramLabel = "PROJECT", description = "Project name.")
    String projectName;

    @Option(names = "--tree", description = "Show the project's content tree instead of a summary.")
    boolean tree;

    @Override
    protected Object query(QueryGateway gateway) {
        return tree ? gateway.getProjectTree(projectName) : gateway.getProject(projectName);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String renderText(Object result) {
        if (result instanceof ProjectDto p) {
            return String.format("%s%s%n  %s%n  goals=%d stories=%d actors=%d useCases=%d "
                    + "scenarios=%d glossary=%d stakeholders=%d",
                    p.name(), p.status() == null ? "" : "  (" + p.status() + ")",
                    p.description() == null ? "(no description)" : p.description(),
                    p.goalCount(), p.storyCount(), p.actorCount(), p.useCaseCount(),
                    p.scenarioCount(), p.glossaryTermCount(), p.stakeholderCount());
        }
        List<ProjectTreeNodeDto> nodes = (List<ProjectTreeNodeDto>) result;
        StringBuilder sb = new StringBuilder();
        renderNodes(nodes, 0, sb);
        return sb.length() == 0 ? "(empty)" : sb.toString();
    }

    private static void renderNodes(List<ProjectTreeNodeDto> nodes, int depth, StringBuilder sb) {
        if (nodes == null) {
            return;
        }
        for (ProjectTreeNodeDto node : nodes) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("  ".repeat(depth)).append("- ").append(node.name());
            if (node.type() != null) {
                sb.append(" [").append(node.type()).append(node.id() != null ? " #" + node.id() : "").append("]");
            }
            renderNodes(node.children(), depth + 1, sb);
        }
    }
}
