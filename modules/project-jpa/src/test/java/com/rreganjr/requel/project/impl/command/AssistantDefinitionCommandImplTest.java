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
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.identity.User;
import com.rreganjr.repository.jpa.BeanValidationException;
import com.rreganjr.requel.project.AssistantDefinition;
import com.rreganjr.requel.project.InvalidDefinitionException;
import com.rreganjr.requel.project.InvalidDefinitionException.Problem;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantDefinitions;
import com.rreganjr.requel.project.ProjectAssistantDefinitions.Draft;
import com.rreganjr.requel.project.ProjectAssistantDefinitions.View;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.validator.EntityValidationException;

/** Issue #264: the definition commands hand over to the store and report its refusals by field. */
class AssistantDefinitionCommandImplTest {

	private final ProjectAssistantDefinitions definitions = mock(ProjectAssistantDefinitions.class);
	private final ProjectAssistantSettingsStore settings = mock(ProjectAssistantSettingsStore.class);
	private final Project project = mock(Project.class);
	private final User ron = mock(User.class);
	private final Draft draft = new Draft("POLICY", "house-style", "House style", Set.of(),
			List.of("entity"), Map.of(), "Check it.", List.of(), false, null);
	private final View view = new View("house-style", "House style", "POLICY", "POLICY_REVIEW",
			Set.of(), List.of("entity"), Map.of(), "Check it.", List.of(), false, "PROJECT", 1,
			null, null, 1, List.of("Goal"));

	@BeforeEach
	void setUp() {
		when(project.getId()).thenReturn(7L);
		when(ron.getUsername()).thenReturn("ron");
	}

	private <T extends AssistantDefinitionCommandImpl> T command(T command, String key,
			Integer lockVersion) {
		command.setDefinitions(definitions);
		command.setSettingsStore(settings);
		command.setProject(project);
		command.setKey(key);
		command.setLockVersion(lockVersion);
		command.setDraft(draft);
		command.setEditedBy(ron);
		return command;
	}

	@Test
	void eachCommandNeedsTheDefinitionPermission() {
		RequiresStakeholderPermission requirement = (RequiresStakeholderPermission)
				new AssistantDefinitionCommandImpl.CreateImpl().getAuthorizationRequirement();
		assertThat(requirement.entityType()).isEqualTo(AssistantDefinition.class);
		assertThat(requirement.permissionType()).isEqualTo("Edit");
	}

	@Test
	void createEditAndForkReturnTheDefinition() throws Exception {
		when(definitions.create(7L, draft, "ron")).thenReturn(view);
		when(definitions.edit(7L, "house-style", 1, draft, "ron")).thenReturn(view);
		when(definitions.fork(7L, "ai-review-goal", "ron")).thenReturn(view);

		var create = command(new AssistantDefinitionCommandImpl.CreateImpl(), null, null);
		create.execute();
		var edit = command(new AssistantDefinitionCommandImpl.EditImpl(), "house-style", 1);
		edit.execute();
		var fork = command(new AssistantDefinitionCommandImpl.ForkImpl(), "ai-review-goal", null);
		fork.execute();

		assertThat(create.getDefinition()).isSameAs(view);
		assertThat(edit.getDefinition()).isSameAs(view);
		assertThat(fork.getDefinition()).isSameAs(view);
		assertThat(create.getProject()).isSameAs(project);
		assertThat(create.getEditedBy()).isSameAs(ron);
	}

	@Test
	void revertAndDeleteTakeTheLockVersionAndDeleteTurnsTheSwitchBackOn() throws Exception {
		var revert = command(new AssistantDefinitionCommandImpl.RevertImpl(), "ai-review-goal", 2);
		revert.execute();
		var delete = command(new AssistantDefinitionCommandImpl.DeleteImpl(), "house-style", 3);
		delete.execute();

		verify(definitions).revert(7L, "ai-review-goal", 2);
		verify(definitions).delete(7L, "house-style", 3);
		verify(settings).setEnabled(7L, "house-style", true, ron);
		assertThat(revert.getDefinition()).isNull();
	}

	@Test
	void aRefusalComesBackAsFieldErrors() {
		when(definitions.create(any(), any(), any())).thenThrow(new InvalidDefinitionException(
				"house-style", List.of(new Problem("instructions", "too long"),
						new Problem(null, "whole"))));

		BeanValidationException e = catchThrowableOfType(
				() -> command(new AssistantDefinitionCommandImpl.CreateImpl(), null, null)
						.execute(), BeanValidationException.class);

		assertThat(e.getEntityPropertyNames()).containsExactly("instructions", "definition");
		assertThat(e.getFieldMessages()).containsExactly("too long", "whole");
	}

	@Test
	void aMissingVersionDraftProjectOrStoreIsRefused() {
		BeanValidationException noVersion = catchThrowableOfType(
				() -> command(new AssistantDefinitionCommandImpl.EditImpl(), "k", null).execute(),
				BeanValidationException.class);
		assertThat(noVersion.getEntityPropertyNames()).containsExactly("version");
		var noDraft = command(new AssistantDefinitionCommandImpl.CreateImpl(), null, null);
		noDraft.setDraft(null);
		assertThat(catchThrowableOfType(noDraft::execute, BeanValidationException.class)
				.getEntityPropertyNames()).containsExactly("definition");
		var noProject = command(new AssistantDefinitionCommandImpl.ForkImpl(), "k", null);
		noProject.setProject(null);
		assertThatThrownBy(noProject::execute).isInstanceOf(EntityValidationException.class);
		var noStore = command(new AssistantDefinitionCommandImpl.RevertImpl(), "k", 1);
		noStore.setDefinitions(null);
		assertThatThrownBy(noStore::execute).isInstanceOf(IllegalStateException.class);
		verify(definitions, org.mockito.Mockito.never()).edit(any(), any(), anyInt(), any(),
				any());
		verifyNoInteractions(settings);
	}

	/** The SPI's defaults, for an implementation without authoring: nothing to read, no writes. */
	@Test
	void withoutAuthoringTheDefinitionsReadEmptyAndRefuseWrites() {
		ProjectAssistantDefinitions none = projectId -> 0;

		assertThat(none.effective(7L)).isEmpty();
		assertThat(none.find(7L, "k")).isEmpty();
		assertThat(none.bundled("k")).isEmpty();
		assertThatThrownBy(() -> none.create(7L, draft, "ron"))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> none.edit(7L, "k", 1, draft, "ron"))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> none.fork(7L, "k", "ron"))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> none.revert(7L, "k", 1))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> none.delete(7L, "k", 1))
				.isInstanceOf(UnsupportedOperationException.class);
		Draft empty = new Draft("POLICY", "k", "K", null, null, null, "x", null, false, null);
		assertThat(empty.scope()).isEmpty();
		assertThat(empty.contextProviders()).isEmpty();
		assertThat(empty.contextBudgets()).isEmpty();
		assertThat(empty.vocabulary()).isEmpty();
		assertThat(new InvalidDefinitionException("k", List.of(new Problem("key", "bad"))).problems())
				.containsExactly("bad");
	}

	/** The factory builds each definition command through its creation strategy. */
	@Test
	void theFactoryMakesEachDefinitionCommand() {
		com.rreganjr.command.CommandFactoryStrategy strategy = mock(
				com.rreganjr.command.CommandFactoryStrategy.class);
		when(strategy.newInstance(any())).thenAnswer(inv -> ((Class<?>) inv.getArgument(0))
				.getDeclaredConstructor().newInstance());
		ProjectCommandFactoryImpl factory = new ProjectCommandFactoryImpl(strategy);

		assertThat(factory.newCreateAssistantDefinitionCommand())
				.isInstanceOf(AssistantDefinitionCommandImpl.CreateImpl.class);
		assertThat(factory.newEditAssistantDefinitionCommand())
				.isInstanceOf(AssistantDefinitionCommandImpl.EditImpl.class);
		assertThat(factory.newForkAssistantDefinitionCommand())
				.isInstanceOf(AssistantDefinitionCommandImpl.ForkImpl.class);
		assertThat(factory.newRevertAssistantDefinitionCommand())
				.isInstanceOf(AssistantDefinitionCommandImpl.RevertImpl.class);
		assertThat(factory.newDeleteAssistantDefinitionCommand())
				.isInstanceOf(AssistantDefinitionCommandImpl.DeleteImpl.class);
	}
}
