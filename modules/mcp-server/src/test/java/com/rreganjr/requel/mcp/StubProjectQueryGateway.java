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
package com.rreganjr.requel.mcp;

import java.util.List;

import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto;

class StubProjectQueryGateway implements ProjectQueryGateway {

	@Override
	public List<ProjectDto> listProjects() {
		return List.of(project());
	}

	@Override
	public ProjectDto getProject(String projectName) {
		return project();
	}

	@Override
	public List<ProjectTreeNodeDto> getProjectTree(String projectName) {
		return List.of(new ProjectTreeNodeDto("Goals", "Goals",
				List.of(new ProjectTreeNodeDto(10L, "Goals", "Improve login"))));
	}

	private ProjectDto project() {
		return new ProjectDto(1L, 2, "Sample", "A sample project", "Requel", "admin",
				"ACTIVE", 1, 2, 3, 4, 5, 6, 7, 8);
	}
}
