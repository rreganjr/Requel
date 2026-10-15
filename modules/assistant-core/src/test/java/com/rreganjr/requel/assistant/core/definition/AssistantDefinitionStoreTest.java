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

import static com.rreganjr.requel.assistant.core.definition.Definitions.TASK;
import static com.rreganjr.requel.assistant.core.definition.Definitions.fallback;
import static com.rreganjr.requel.assistant.core.definition.Definitions.project;
import static com.rreganjr.requel.assistant.core.definition.Definitions.review;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.core.persistence.AssistantDefinitionEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantDefinitionRepository;

/** #260: the definition store - cache, project override, seeding by version, evict on write. */
class AssistantDefinitionStoreTest {

	private final List<AssistantDefinitionEntity> rows = new ArrayList<>();
	private final AssistantDefinitionRepository repository = mock(AssistantDefinitionRepository.class);
	private AssistantDefinitionStore store;

	@BeforeEach
	void setUp() {
		when(repository.save(any())).thenAnswer(inv -> {
			AssistantDefinitionEntity entity = inv.getArgument(0);
			if (!rows.contains(entity)) {
				rows.add(entity);
			}
			return entity;
		});
		when(repository.findByProjectIdIsNull()).thenAnswer(inv -> rows.stream()
				.filter(r -> r.getProjectId() == null).toList());
		when(repository.findVisibleTo(anyLong())).thenAnswer(inv -> rows.stream()
				.filter(r -> r.getProjectId() == null
						|| Objects.equals(r.getProjectId(), inv.getArgument(0)))
				.toList());
		when(repository.findByDefinitionKeyAndProjectIdIsNull(anyString())).thenAnswer(inv -> rows
				.stream().filter(r -> r.getProjectId() == null
						&& r.getDefinitionKey().equals(inv.getArgument(0)))
				.findFirst());
		when(repository.findByDefinitionKeyAndProjectId(anyString(), anyLong())).thenAnswer(
				inv -> rows.stream().filter(r -> Objects.equals(r.getProjectId(),
						inv.getArgument(1)) && r.getDefinitionKey().equals(inv.getArgument(0)))
						.findFirst());
		when(repository.deleteByProjectId(anyLong())).thenAnswer(inv -> {
			List<AssistantDefinitionEntity> owned = rows.stream()
					.filter(r -> Objects.equals(r.getProjectId(), inv.getArgument(0))).toList();
			rows.removeAll(owned);
			return owned.size();
		});
		store = new AssistantDefinitionStore(repository, new ObjectMapper(),
				new AssistantDefinitionValidator(16000),
				Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void aSavedDefinitionReadsBackWhole() {
		AssistantDefinition saved = review("goals", Set.of("Goal", "Story"));
		store.save(saved, "ron");

		assertThat(store.definitionsFor(5L, TASK)).containsExactly(saved);
		assertThat(rows.get(0).getCreatedBy()).isEqualTo("ron");
		assertThat(store.definitionsFor(5L, "OTHER")).isEmpty();
	}

	@Test
	void contextBudgetsReadBack() {
		AssistantDefinition f = review("goals", Set.of("Goal"));
		AssistantDefinition saved = new AssistantDefinition(f.key(), f.displayName(), f.kind(),
				f.taskType(), f.scope(), java.util.List.of("entity", "goal-siblings"),
				f.instructions(), f.vocabulary(), f.outputSchemaName(), f.outputSchemaVersion(),
				f.enabled(), f.version(), f.source(), f.projectId(), f.forkedFromVersion(),
				f.executorBean(), java.util.Map.of("goal-siblings", 2000));
		store.save(saved, "ron");

		assertThat(rows.get(0).getContextBudgetsJson()).isEqualTo("{\"goal-siblings\":2000}");
		assertThat(store.definitionsFor(5L, TASK).get(0).contextBudgets())
				.containsExactly(java.util.Map.entry("goal-siblings", 2000));
		assertThat(review("plain", Set.of("Story")).contextBudgets()).isEmpty();
	}

	/** #265: a local-only policy reads back as one, through the row and the bundled JSON. */
	@Test
	void aLocalOnlyPolicyReadsBack() throws Exception {
		AssistantDefinition saved = Definitions.policy("pii", Set.of(), true);
		store.save(saved, "ron");

		assertThat(rows.get(0).isLocalOnly()).isTrue();
		assertThat(store.definitionsFor(5L, AssistantDefinition.POLICY_REVIEW))
				.containsExactly(saved);
	}

	@Test
	void aDevOverrideReplacesWhateverTheVersionAndTheNextSeedPutsTheShippedFileBack() {
		store.seedBundled(new AssistantDefinition("default", "D", DefinitionKind.REVIEW, TASK,
				Set.of(), java.util.List.of("entity"), "Shipped.", fallback("default").vocabulary(),
				"RequirementsReviewOutput", "1", true, 5, DefinitionSource.BUNDLED, null, null, null));
		store.overrideBundled(fallback("default")); // version 1 < 5, replaced anyway
		assertThat(store.bundled().get(0).instructions()).isEqualTo("Review the entity.");

		AssistantDefinition shipped = new AssistantDefinition("default", "D", DefinitionKind.REVIEW,
				TASK, Set.of(), java.util.List.of("entity"), "Shipped.", fallback("default").vocabulary(),
				"RequirementsReviewOutput", "1", true, 5, DefinitionSource.BUNDLED, null, null, null);
		assertThat(store.seedBundled(shipped)).isTrue(); // same version, but the row was a dev override
		assertThat(store.bundled().get(0).instructions()).isEqualTo("Shipped.");
		assertThat(store.seedBundled(shipped)).isFalse(); // a normal row again
	}

	@Test
	void devOverridesAreReadFromADirectory(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
			throws Exception {
		store.seedBundled(fallback("default"));
		java.nio.file.Files.writeString(dir.resolve("default.json"), """
				{"key":"default","displayName":"D","taskType":"REVIEW","version":1,
				 "contextProviders":["entity"],"outputSchemaName":"RequirementsReviewOutput",
				 "outputSchemaVersion":"1","vocabulary":[{"type":"AMBIGUOUS","description":"u"}],
				 "instructions":"Tuned."}
				""".replace("\"REVIEW\"", "\"" + TASK + "\""));
		DevDefinitionOverrides overrides = new DevDefinitionOverrides(store, new ObjectMapper(),
				dir.toString());

		assertThat(overrides.reload()).containsExactly("default");
		assertThat(store.bundled().get(0).instructions()).isEqualTo("Tuned.");

		java.nio.file.Files.writeString(dir.resolve("broken.json"), "{\"key\":\"x\"}");
		org.assertj.core.api.Assertions.assertThatThrownBy(overrides::reload)
				.isInstanceOf(IllegalStateException.class);
		assertThat(store.bundled()).extracting(AssistantDefinition::key).containsExactly("default");

		DevDefinitionOverrides missing = new DevDefinitionOverrides(store, new ObjectMapper(),
				dir.resolve("nope").toString());
		org.assertj.core.api.Assertions.assertThatThrownBy(missing::reload)
				.hasMessageContaining("is not a directory");
	}

	@Test
	void aBulkReviewReadsTheDefinitionsOncePerProject() {
		store.seedBundled(fallback("default"));
		for (int i = 0; i < 20; i++) {
			store.definitionsFor(5L, TASK);
		}
		verify(repository, times(1)).findVisibleTo(5L);
	}

	@Test
	void aWriteEvictsSoTheNextRunSeesIt() {
		store.seedBundled(fallback("default"));
		assertThat(store.definitionsFor(5L, TASK)).extracting(AssistantDefinition::enabled)
				.containsExactly(true);

		store.save(fallback("default").withEnabled(false), null);

		assertThat(store.definitionsFor(5L, TASK)).extracting(AssistantDefinition::enabled)
				.containsExactly(false);
	}

	@Test
	void aProjectDefinitionOverridesTheBundledOneForThatProjectOnly() {
		store.seedBundled(fallback("default"));
		AssistantDefinition forked = fallback("default").forkFor(7L, "Project 7's prompt.");
		store.save(forked, "ron");

		assertThat(store.definitionsFor(7L, TASK)).extracting(AssistantDefinition::instructions)
				.containsExactly("Project 7's prompt.");
		assertThat(store.definitionsFor(8L, TASK)).extracting(AssistantDefinition::instructions)
				.containsExactly("Review the entity.");
		assertThat(store.definitionsFor(7L, TASK).get(0).forkedFromVersion()).isEqualTo(1);
	}

	@Test
	void deletingAProjectsDefinitionsLeavesTheBundledOnesAndEvicts() {
		store.seedBundled(fallback("default"));
		store.save(fallback("default").forkFor(7L, "Project 7's prompt."), "ron");
		assertThat(store.definitionsFor(7L, TASK)).extracting(AssistantDefinition::instructions)
				.containsExactly("Project 7's prompt.");

		assertThat(store.deleteForProject(7L)).isEqualTo(1);

		assertThat(store.definitionsFor(7L, TASK)).extracting(AssistantDefinition::instructions)
				.containsExactly("Review the entity.");
		assertThat(store.bundled()).hasSize(1);
		assertThat(store.deleteForProject(null)).isZero();
	}

	@Test
	void seedingInsertsUpgradesAndOtherwiseLeavesTheRowAlone() {
		assertThat(store.seedBundled(fallback("default"))).isTrue();
		assertThat(store.seedBundled(fallback("default"))).isFalse();
		AssistantDefinition f = fallback("default");
		AssistantDefinition v2 = new AssistantDefinition(f.key(), f.displayName(), f.kind(),
				f.taskType(), f.scope(), f.contextProviders(), "Version two.", f.vocabulary(),
				f.outputSchemaName(), f.outputSchemaVersion(), true, 2, DefinitionSource.BUNDLED,
				null, null, null);
		assertThat(store.seedBundled(v2)).isTrue();
		assertThat(store.bundled()).extracting(AssistantDefinition::instructions)
				.containsExactly("Version two.");
		assertThat(rows).hasSize(1);
	}

	@Test
	void seedingNeverTouchesAProjectDefinition() {
		assertThatThrownBy(() -> store.seedBundled(project(fallback("default"), 7L)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void anInvalidDefinitionIsNotSaved() {
		store.seedBundled(fallback("default"));
		assertThatThrownBy(() -> store.save(project(fallback("second"), 7L), "ron"))
				.isInstanceOf(InvalidAssistantDefinitionException.class)
				.hasMessageContaining("already has a fallback");
		assertThat(rows).hasSize(1);
	}

	@Test
	void theTokenBudgetIsReadWithRelaxedBinding() {
		org.springframework.mock.env.MockEnvironment env = new org.springframework.mock.env
				.MockEnvironment().withProperty("requel.ai.maxInputTokens", "250");
		assertThat(AssistantDefinitionStore.maxInputTokens(env)).isEqualTo(250);
		assertThat(AssistantDefinitionStore.maxInputTokens(
				new org.springframework.mock.env.MockEnvironment())).isEqualTo(16000);
		assertThat(Optional.ofNullable(AssistantDefinitionStore.maxInputTokens(null)))
				.contains(16000);
	}
}
