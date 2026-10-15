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
package com.rreganjr.requel.assistant.legacynlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.ProjectRepository;

class ProjectEntityTargetLoaderTest {

	private final ProjectRepository projectRepository = mock(ProjectRepository.class);
	private final ProjectEntityTargetLoader loader = new ProjectEntityTargetLoader(
			projectRepository);

	@Test
	void supportsKnownTypesWithAnId() {
		assertThat(loader.supports(EntityRef.of("Goal", 1L))).isTrue();
		assertThat(loader.supports(EntityRef.of("Story", 2L))).isTrue();
		assertThat(loader.supports(EntityRef.of("Unknown", 3L))).isFalse();
		assertThat(loader.supports(null)).isFalse();
	}

	@Test
	void loadsEntityByIdThroughRepository() {
		Goal goal = mock(Goal.class);
		when(projectRepository.findById(Goal.class, 42L)).thenReturn(goal);

		Optional<Object> loaded = loader.loadTarget(EntityRef.of("Goal", 42L));

		assertThat(loaded).containsSame(goal);
	}

	@Test
	void returnsEmptyForUnknownTypeOrMissingEntity() {
		assertThat(loader.loadTarget(EntityRef.of("Unknown", 1L))).isEmpty();

		when(projectRepository.findById(Goal.class, 99L))
				.thenThrow(NoSuchEntityException.byQuery(Goal.class, new String[] { "id" },
						new Object[] { 99L }));
		assertThat(loader.loadTarget(EntityRef.of("Goal", 99L))).isEmpty();
	}
}
