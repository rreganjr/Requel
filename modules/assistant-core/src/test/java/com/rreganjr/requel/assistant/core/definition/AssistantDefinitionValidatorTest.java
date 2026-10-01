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
package com.rreganjr.requel.assistant.core.definition;

import static com.rreganjr.requel.assistant.core.definition.Definitions.fallback;
import static com.rreganjr.requel.assistant.core.definition.Definitions.project;
import static com.rreganjr.requel.assistant.core.definition.Definitions.review;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** #260: what a definition must meet to be saved or seeded. */
class AssistantDefinitionValidatorTest {

	private final AssistantDefinitionValidator validator = new AssistantDefinitionValidator(1000);

	@Test
	void aWellFormedDefinitionPasses() {
		assertThatCode(() -> validator.validate(fallback("default"), List.of()))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validate(review("goals", Set.of("Goal", "Story")),
				List.of(fallback("default")))).doesNotThrowAnyException();
	}

	@Test
	void anUnknownEntityTypeIsRejected() {
		assertThatThrownBy(() -> validator.validate(review("x", Set.of("Goal", "Widget")),
				List.of()))
				.isInstanceOf(InvalidAssistantDefinitionException.class)
				.hasMessageContaining("unknown entity type(s) in scope: [Widget]");
	}

	@Test
	void anUnknownContextProviderIsRejected() {
		AssistantDefinition d = withProviders(fallback("x"), List.of("entity", "siblings"));
		assertThatThrownBy(() -> validator.validate(d, List.of()))
				.hasMessageContaining("unknown context provider(s): [siblings]");
	}

	@Test
	void policyIsRejectedUntilItsRuntimeExists() {
		AssistantDefinition f = fallback("x");
		AssistantDefinition policy = new AssistantDefinition(f.key(), f.displayName(),
				DefinitionKind.POLICY, f.taskType(), f.scope(), f.contextProviders(),
				f.instructions(), f.vocabulary(), f.outputSchemaName(), f.outputSchemaVersion(),
				true, 1, DefinitionSource.BUNDLED, null, null, null);
		assertThatThrownBy(() -> validator.validate(policy, List.of()))
				.hasMessageContaining("kind POLICY is not supported yet");
	}

	@Test
	void vocabularySchemaAndInstructionsAreChecked() {
		AssistantDefinition f = fallback("x");
		AssistantDefinition bad = new AssistantDefinition(f.key(), f.displayName(), f.kind(),
				f.taskType(), f.scope(), f.contextProviders(), "x".repeat(4001), List.of(),
				"OtherOutput", "9", true, 1, DefinitionSource.BUNDLED, null, null, null);
		InvalidAssistantDefinitionException e = (InvalidAssistantDefinitionException) org.assertj
				.core.api.Assertions.catchThrowable(() -> validator.validate(bad, List.of()));
		assertThat(e.problems()).anyMatch(p -> p.contains("vocabulary is empty"))
				.anyMatch(p -> p.contains("output schema OtherOutput v9"))
				.anyMatch(p -> p.contains("instructions are 4001 characters"));
	}

	@Test
	void aRepeatedVocabularyTypeIsRejected() {
		AssistantDefinition f = fallback("x");
		AssistantDefinition bad = new AssistantDefinition(f.key(), f.displayName(), f.kind(),
				f.taskType(), f.scope(), f.contextProviders(), f.instructions(),
				List.of(new VocabularyEntry("A", "a"), new VocabularyEntry("A", "again")),
				f.outputSchemaName(), f.outputSchemaVersion(), true, 1, DefinitionSource.BUNDLED,
				null, null, null);
		assertThatThrownBy(() -> validator.validate(bad, List.of()))
				.hasMessageContaining("vocabulary type A is listed twice");
	}

	@Test
	void twoFallbacksInOneTaskCollide() {
		assertThatThrownBy(() -> validator.validate(fallback("second"),
				List.of(fallback("first"))))
				.hasMessageContaining("already has a fallback definition (first)");
	}

	@Test
	void twoSpecificDefinitionsOnOneTypeCollide() {
		assertThatThrownBy(() -> validator.validate(review("b", Set.of("Goal", "Actor")),
				List.of(review("a", Set.of("Goal", "Story")))))
				.hasMessageContaining("scope [Goal] collides with definition a");
	}

	@Test
	void theDefinitionBeingReplacedIsNotACollision() {
		assertThatCode(() -> validator.validate(fallback("default"), List.of(fallback("default"))))
				.doesNotThrowAnyException();
		// A project definition overriding the bundled one with the same key.
		assertThatCode(() -> validator.validate(project(fallback("default"), 7L),
				List.of(fallback("default")))).doesNotThrowAnyException();
	}

	@Test
	void anotherTasksDefinitionsNeverCollide() {
		AssistantDefinition f = fallback("other");
		AssistantDefinition otherTask = new AssistantDefinition(f.key(), f.displayName(), f.kind(),
				"OTHER_TASK", f.scope(), f.contextProviders(), f.instructions(), f.vocabulary(),
				f.outputSchemaName(), f.outputSchemaVersion(), true, 1, DefinitionSource.BUNDLED,
				null, null, null);
		assertThatCode(() -> validator.validate(otherTask, List.of(fallback("default"))))
				.doesNotThrowAnyException();
	}

	@Test
	void ownershipMustMatchTheSource() {
		AssistantDefinition f = fallback("x");
		AssistantDefinition orphan = new AssistantDefinition(f.key(), f.displayName(), f.kind(),
				f.taskType(), f.scope(), f.contextProviders(), f.instructions(), f.vocabulary(),
				f.outputSchemaName(), f.outputSchemaVersion(), true, 1, DefinitionSource.PROJECT,
				null, null, null);
		assertThatThrownBy(() -> validator.validate(orphan, List.of()))
				.hasMessageContaining("a PROJECT definition needs an owning project");
	}

	private static AssistantDefinition withProviders(AssistantDefinition f, List<String> providers) {
		return new AssistantDefinition(f.key(), f.displayName(), f.kind(), f.taskType(), f.scope(),
				providers, f.instructions(), f.vocabulary(), f.outputSchemaName(),
				f.outputSchemaVersion(), f.enabled(), f.version(), f.source(), f.projectId(),
				f.forkedFromVersion(), f.executorBean());
	}
}
