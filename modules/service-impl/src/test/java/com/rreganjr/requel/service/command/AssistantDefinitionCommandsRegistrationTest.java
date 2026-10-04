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
package com.rreganjr.requel.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rreganjr.command.Command;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantDefinitions.View;
import com.rreganjr.requel.project.ProjectAssistantDefinitions.Vocabulary;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.AssistantDefinitionCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.service.api.CommandRegistration;
import com.rreganjr.requel.service.api.dto.AssistantDefinitionVocabularyInput;
import com.rreganjr.requel.service.api.dto.CreateAssistantDefinitionInput;
import com.rreganjr.requel.service.api.dto.DeleteAssistantDefinitionInput;
import com.rreganjr.requel.service.api.dto.EditAssistantDefinitionInput;
import com.rreganjr.requel.service.api.dto.ForkAssistantDefinitionInput;
import com.rreganjr.requel.service.api.dto.ProjectDefinitionDto;
import com.rreganjr.requel.service.api.dto.RevertAssistantDefinitionInput;

/** Issue #264: each definition command's input reaches its command, and its result the API. */
class AssistantDefinitionCommandsRegistrationTest {

	private final ProjectCommandFactory factory = mock(ProjectCommandFactory.class);
	private final ProjectRepository projects = mock(ProjectRepository.class);
	private final CommandRegistryImpl registry = new CommandRegistryImpl();
	private final Project project = mock(Project.class);
	private final View view = new View("house-style", "House style", "POLICY", "POLICY_REVIEW",
			Set.of(), List.of("entity"), Map.of(), "x",
			List.of(new Vocabulary("RULE_BROKEN", "broken", "quality")), false, "PROJECT", 1, null,
			null, 1, List.of("Goal"));

	@BeforeEach
	void setUp() throws Exception {
		when(projects.findProjectByName("p")).thenReturn(project);
		new ProjectCommandRegistrar(factory, projects, registry, mock(DictionaryRepository.class))
				.registerCommands();
	}

	@SuppressWarnings("unchecked")
	private <T> Object run(String type, Command command, T input) {
		CommandRegistration<T> registration = (CommandRegistration<T>) registry.lookup(type);
		assertThat(registration.factoryMethod().get()).isSameAs(command);
		registration.inputApplicator().accept(command, input);
		return registration.resultExtractor() == null ? null
				: registration.resultExtractor().apply(command);
	}

	@Test
	void createAndEditCarryTheDraftAndReturnTheDefinition() {
		AssistantDefinitionCommand.Create create = mock(AssistantDefinitionCommand.Create.class);
		when(factory.newCreateAssistantDefinitionCommand()).thenReturn(create);
		when(create.getDefinition()).thenReturn(view);
		AssistantDefinitionCommand.Edit edit = mock(AssistantDefinitionCommand.Edit.class);
		when(factory.newEditAssistantDefinitionCommand()).thenReturn(edit);
		List<AssistantDefinitionVocabularyInput> vocabulary = java.util.Arrays.asList(
				new AssistantDefinitionVocabularyInput("RULE_BROKEN", "broken", null), null);

		Object created = run("CreateAssistantDefinition", create,
				new CreateAssistantDefinitionInput("p", "POLICY", "house-style", "House style",
						Set.of(), List.of("entity"), Map.of(), "x", vocabulary, true, null));
		run("EditAssistantDefinition", edit, new EditAssistantDefinitionInput("p", "house-style", 3,
				null, "House style", Set.of("Goal"), List.of("entity"), Map.of(), "y", null, false,
				"bean"));

		verify(create).setProject(project);
		verify(create).setDraft(argThat(d -> d.kind().equals("POLICY")
				&& d.key().equals("house-style") && d.localOnly() && d.vocabulary().size() == 2
				&& d.vocabulary().get(0).type().equals("RULE_BROKEN")
				&& d.vocabulary().get(1) == null));
		assertThat(((ProjectDefinitionDto) created).key()).isEqualTo("house-style");
		assertThat(((ProjectDefinitionDto) created).enabled()).isNull();
		verify(edit).setLockVersion(3);
		verify(edit).setKey("house-style");
		verify(edit).setDraft(argThat(d -> d.instructions().equals("y")
				&& d.executorBean().equals("bean") && d.vocabulary().isEmpty()));
	}

	@Test
	void forkRevertAndDeleteNameTheDefinition() {
		AssistantDefinitionCommand.Fork fork = mock(AssistantDefinitionCommand.Fork.class);
		when(factory.newForkAssistantDefinitionCommand()).thenReturn(fork);
		AssistantDefinitionCommand.Revert revert = mock(AssistantDefinitionCommand.Revert.class);
		when(factory.newRevertAssistantDefinitionCommand()).thenReturn(revert);
		AssistantDefinitionCommand.Delete delete = mock(AssistantDefinitionCommand.Delete.class);
		when(factory.newDeleteAssistantDefinitionCommand()).thenReturn(delete);

		assertThat(run("ForkAssistantDefinition", fork,
				new ForkAssistantDefinitionInput("p", "ai-review-goal"))).isNull();
		assertThat(run("RevertAssistantDefinition", revert,
				new RevertAssistantDefinitionInput("p", "ai-review-goal", 2))).isNull();
		assertThat(run("DeleteAssistantDefinition", delete,
				new DeleteAssistantDefinitionInput("p", "house-style", 4))).isNull();

		verify(fork).setKey("ai-review-goal");
		verify(revert).setLockVersion(2);
		verify(delete).setKey("house-style");
		verify(delete).setLockVersion(4);
		verify(delete).setProject(project);
	}
}
