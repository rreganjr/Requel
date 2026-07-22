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
import java.util.List;
import picocli.CommandLine.Command;

/** {@code requel projects} — list the projects the authenticated user can see. */
@Command(name = "projects", description = "List projects visible to you.")
public class ProjectsCommand extends AbstractQueryCommand {

    @Override
    protected Object query(QueryGateway gateway) {
        return gateway.listProjects();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String renderText(Object result) {
        List<ProjectDto> projects = (List<ProjectDto>) result;
        if (projects.isEmpty()) {
            return "No projects.";
        }
        int width = projects.stream().mapToInt(p -> p.name().length()).max().orElse(0);
        StringBuilder sb = new StringBuilder();
        for (ProjectDto p : projects) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(String.format("%-" + width + "s  %s  (goals=%d stories=%d actors=%d useCases=%d)",
                    p.name(), p.status() == null ? "" : p.status(),
                    p.goalCount(), p.storyCount(), p.actorCount(), p.useCaseCount()));
        }
        return sb.toString();
    }
}
