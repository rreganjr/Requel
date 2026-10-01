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

import org.junit.jupiter.api.Test;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.validator.EntityValidationException;

/** Issue #262: what the data-handling command refuses before it touches the store. */
class EditProjectDataHandlingSettingCommandImplTest {

	private final ProjectAssistantSettingsStore store = mock(ProjectAssistantSettingsStore.class);
	private final Project project = mock(Project.class);
	private final User ron = mock(User.class);

	@Test
	void setsEgressAndRedactionKeys() throws Exception {
		when(project.getId()).thenReturn(7L);
		EditProjectDataHandlingSettingCommandImpl egress = command(project, "egress.external");
		egress.setEnabled(false);
		egress.execute();
		verify(store).setEnabled(7L, "egress.external", false, ron);

		EditProjectDataHandlingSettingCommandImpl email = command(project, "redaction.email");
		email.setEnabled(false);
		email.execute();
		verify(store).setEnabled(7L, "redaction.email", false, ron);
	}

	@Test
	void aProjectAndAKeyAreRequired() {
		assertThatThrownBy(() -> command(null, "egress.external").execute())
				.isInstanceOf(EntityValidationException.class);
		assertThatThrownBy(() -> command(project, null).execute())
				.isInstanceOf(EntityValidationException.class);
		assertThatThrownBy(() -> command(project, " ").execute())
				.isInstanceOf(EntityValidationException.class);
		verifyNoInteractions(store);
	}

	@Test
	void aKeyOutsideTheVocabularyIsRefused() {
		// an assistant id is not a data-handling key, and an unknown category is refused
		assertThatThrownBy(() -> command(project, "legacy-lexical-spelling").execute())
				.isInstanceOf(EntityValidationException.class);
		assertThatThrownBy(() -> command(project, "redaction.passport").execute())
				.isInstanceOf(EntityValidationException.class);
		verifyNoInteractions(store);
	}

	@Test
	void withoutAStoreTheCommandFails() {
		EditProjectDataHandlingSettingCommandImpl command = command(project, "egress.external");
		command.setSettingsStore(null);
		assertThatThrownBy(command::execute).isInstanceOf(IllegalStateException.class);
	}

	private EditProjectDataHandlingSettingCommandImpl command(Project project, String key) {
		EditProjectDataHandlingSettingCommandImpl command =
				new EditProjectDataHandlingSettingCommandImpl(null, null, null, null, null, null);
		command.setSettingsStore(store);
		command.setProject(project);
		command.setKey(key);
		command.setEditedBy(ron);
		return command;
	}
}
