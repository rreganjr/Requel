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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.SwitchableAssistantCatalog;
import com.rreganjr.requel.project.SwitchableAssistantCatalog.SwitchableAssistant;
import com.rreganjr.validator.EntityValidationException;

/** Issue #268: what the command refuses before it touches the store. */
class EditProjectAssistantSettingCommandImplTest {

	private final ProjectAssistantSettingsStore store = mock(ProjectAssistantSettingsStore.class);
	private final SwitchableAssistantCatalog catalog = () -> List
			.of(new SwitchableAssistant("legacy-lexical-spelling", "Spelling"));
	private final Project project = mock(Project.class);
	private final User ron = mock(User.class);

	@Test
	void setsTheSettingForAKnownAssistant() throws Exception {
		when(project.getId()).thenReturn(7L);
		EditProjectAssistantSettingCommandImpl command = command(project, "legacy-lexical-spelling");
		command.setEnabled(false);

		command.execute();

		verify(store).setEnabled(7L, "legacy-lexical-spelling", false, ron);
	}

	@Test
	void aProjectAndAnAssistantIdAreRequired() {
		assertThatThrownBy(() -> command(null, "legacy-lexical-spelling").execute())
				.isInstanceOf(EntityValidationException.class);
		assertThatThrownBy(() -> command(project, null).execute())
				.isInstanceOf(EntityValidationException.class);
		assertThatThrownBy(() -> command(project, "  ").execute())
				.isInstanceOf(EntityValidationException.class);
		verifyNoInteractions(store);
	}

	@Test
	void anAssistantTheCatalogDoesNotListIsRefused() {
		assertThatThrownBy(() -> command(project, "legacy-lexical").execute())
				.isInstanceOf(EntityValidationException.class);
		EditProjectAssistantSettingCommandImpl noCatalog = command(project,
				"legacy-lexical-spelling");
		noCatalog.setCatalog(null);
		assertThatThrownBy(noCatalog::execute).isInstanceOf(EntityValidationException.class);
		verifyNoInteractions(store);
	}

	@Test
	void withoutAStoreTheCommandFails() {
		EditProjectAssistantSettingCommandImpl command = command(project, "legacy-lexical-spelling");
		command.setSettingsStore(null);
		assertThatThrownBy(command::execute).isInstanceOf(IllegalStateException.class);
	}

	private EditProjectAssistantSettingCommandImpl command(Project project, String assistantId) {
		EditProjectAssistantSettingCommandImpl command = new EditProjectAssistantSettingCommandImpl(
				null, null, null, null, null, null);
		command.setSettingsStore(store);
		command.setCatalog(catalog);
		command.setProject(project);
		command.setAssistantId(assistantId);
		command.setEditedBy(ron);
		return command;
	}
}
