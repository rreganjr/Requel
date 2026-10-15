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
package com.rreganjr.requel.project.impl.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.validator.EntityValidationException;

class AnalyzeProjectCommandImplTest {

	private final ProjectRepository projectRepository = mock(ProjectRepository.class);
	private final AnalyzeProjectCommandImpl command = new AnalyzeProjectCommandImpl(null, null,
			projectRepository, null, null, null);

	@Test
	void theProjectIsRequired() {
		assertThatThrownBy(command::execute).isInstanceOf(EntityValidationException.class);
	}

	@Test
	void theProjectIsReloadedAndAnalyzedAsTheEditor() throws Exception {
		Project detached = mock(Project.class);
		Project loaded = mock(Project.class);
		when(projectRepository.get(detached)).thenReturn(loaded);
		User ron = mock(User.class);
		command.setProject(detached);
		command.setEditedBy(ron);

		command.execute();

		assertThat(command.getAnalysisProject()).isSameAs(loaded);
		assertThat(command.getAnalysisTriggeredBy()).isSameAs(ron);
	}
}
