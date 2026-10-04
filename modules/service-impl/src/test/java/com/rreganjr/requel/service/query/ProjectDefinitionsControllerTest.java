/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.rreganjr.requel.project.AssistantDefinition;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantDefinitions;
import com.rreganjr.requel.project.ProjectAssistantDefinitions.View;
import com.rreganjr.requel.project.ProjectAssistantDefinitions.Vocabulary;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.service.api.dto.ProjectDefinitionDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;

/** Issue #264: the definitions reads need AssistantDefinition[Edit] and show each one's switch. */
class ProjectDefinitionsControllerTest {

	private final ProjectRepository projects = mock(ProjectRepository.class);
	private final CurrentUserResolver users = mock(CurrentUserResolver.class);
	private final ProjectAssistantDefinitions definitions = mock(ProjectAssistantDefinitions.class);
	private final ProjectAssistantSettingsStore settings = mock(ProjectAssistantSettingsStore.class);
	private final Project project = mock(Project.class);
	private final User user = mock(User.class);
	private ProjectDefinitionsController controller;

	private static View view(String key, String source, Integer bundledVersion) {
		return new View(key, key, "REVIEW", "REQUIREMENTS_REVIEW", Set.of("Goal"),
				List.of("entity"), Map.of(), "x", List.of(new Vocabulary("A", "a", "quality")),
				false, source, 2, 1, bundledVersion, 3, List.of("Goal"));
	}

	@BeforeEach
	void setUp() throws Exception {
		when(project.getId()).thenReturn(7L);
		when(projects.findProjectByName("p")).thenReturn(project);
		when(users.resolve()).thenReturn(user);
		controller = new ProjectDefinitionsController(projects, users);
		controller.setDefinitions(definitions);
		controller.setSettingsStore(settings);
	}

	private void grant(boolean definitionEdit) {
		UserStakeholder stakeholder = mock(UserStakeholder.class);
		StakeholderPermission permission = mock(StakeholderPermission.class);
		when(permission.getPermissionKey()).thenReturn(definitionEdit
				? AssistantDefinition.class.getName() + "[Edit]"
				: Project.class.getName() + "[Edit]");
		when(stakeholder.getStakeholderPermissions()).thenReturn(Set.of(permission));
		when(project.getUserStakeholder(any())).thenReturn(stakeholder);
	}

	@Test
	void theListShowsEachDefinitionWithItsSwitch() {
		grant(true);
		when(definitions.effective(7L)).thenReturn(List.of(view("goals", "PROJECT", 3),
				view("house-style", "PROJECT", null)));
		when(settings.disabledAssistants(7L)).thenReturn(Set.of("house-style"));

		ResponseEntity<?> response = controller.list("p");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		@SuppressWarnings("unchecked")
		List<ProjectDefinitionDto> body = (List<ProjectDefinitionDto>) response.getBody();
		assertThat(body).extracting(ProjectDefinitionDto::key, ProjectDefinitionDto::enabled,
				ProjectDefinitionDto::bundledVersion).containsExactly(
						org.assertj.core.groups.Tuple.tuple("goals", true, 3),
						org.assertj.core.groups.Tuple.tuple("house-style", false, null));
		assertThat(body.get(0).vocabulary()).singleElement().satisfies(v -> {
			assertThat(v.type()).isEqualTo("A");
			assertThat(v.category()).isEqualTo("quality");
		});
	}

	@Test
	void oneDefinitionComesWithItsBundledBaseline() {
		grant(true);
		when(definitions.find(7L, "goals")).thenReturn(Optional.of(view("goals", "PROJECT", 3)));
		when(definitions.bundled("goals")).thenReturn(Optional.of(view("goals", "BUNDLED", 3)));
		when(settings.disabledAssistants(7L)).thenReturn(Set.of());

		ResponseEntity<?> response = controller.get("p", "goals");

		ProjectDefinitionsController.DefinitionDetailDto body =
				(ProjectDefinitionsController.DefinitionDetailDto) response.getBody();
		assertThat(body.definition().source()).isEqualTo("PROJECT");
		assertThat(body.definition().enabled()).isTrue();
		assertThat(body.bundled().source()).isEqualTo("BUNDLED");
		assertThat(body.bundled().enabled()).isNull();
		assertThat(controller.get("p", "missing").getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void withoutThePermissionBothReadsAreForbidden() {
		grant(false);

		assertThat(controller.list("p").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(controller.get("p", "goals").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void anUnknownProjectOrNoDefinitionsReadsSafely() throws Exception {
		when(projects.findProjectByName("nope")).thenThrow(
				NoSuchProjectException.forName("nope"));
		assertThat(controller.list("nope").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(controller.get("nope", "k").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		grant(true);
		controller.setDefinitions(null);
		controller.setSettingsStore(null);
		assertThat(controller.list("p").getBody()).isEqualTo(List.of());
		assertThat(controller.get("p", "goals").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
