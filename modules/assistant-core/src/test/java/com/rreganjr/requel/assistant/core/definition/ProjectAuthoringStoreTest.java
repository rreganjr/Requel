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
import static com.rreganjr.requel.assistant.core.definition.Definitions.review;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.platform.exception.EntityLockException;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.core.persistence.AssistantDefinitionEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantDefinitionRepository;
import com.rreganjr.requel.project.InvalidDefinitionException;
import com.rreganjr.requel.project.InvalidDefinitionException.Problem;
import com.rreganjr.requel.project.ProjectAssistantDefinitions.Draft;
import com.rreganjr.requel.project.ProjectAssistantDefinitions.View;
import com.rreganjr.requel.project.ProjectAssistantDefinitions.Vocabulary;

/**
 * Issue #264: a project authors its own definitions through the store - create, fork, edit,
 * revert, delete - with the project-only rules and the optimistic lock.
 */
class ProjectAuthoringStoreTest {

	private static final long PROJECT = 5L;
	private static final long OTHER = 6L;

	private final List<AssistantDefinitionEntity> rows = new ArrayList<>();
	private final AssistantDefinitionRepository repository = mock(AssistantDefinitionRepository.class);
	private AssistantDefinitionStore store;

	@BeforeEach
	void setUp() {
		when(repository.save(any())).thenAnswer(inv -> keep(inv.getArgument(0)));
		when(repository.saveAndFlush(any())).thenAnswer(inv -> {
			AssistantDefinitionEntity entity = keep(inv.getArgument(0));
			// what JPA's @Version does on flush
			ReflectionTestUtils.setField(entity, "lockVersion", entity.getLockVersion() + 1);
			return entity;
		});
		doAnswer(inv -> rows.remove(inv.getArgument(0))).when(repository).delete(any());
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
		when(repository.countByProjectId(anyLong())).thenAnswer(inv -> rows.stream()
				.filter(r -> Objects.equals(r.getProjectId(), inv.getArgument(0))).count());
		store = new AssistantDefinitionStore(repository, new ObjectMapper(),
				new AssistantDefinitionValidator(16000),
				Clock.fixed(Instant.parse("2026-10-04T00:00:00Z"), ZoneOffset.UTC));
		// bundled: a goal review at version 3 and the review fallback
		AssistantDefinition goals = review("ai-review-goal", Set.of("Goal"));
		store.seedBundled(new AssistantDefinition(goals.key(), "Goal review", goals.kind(),
				goals.taskType(), goals.scope(), goals.contextProviders(), goals.instructions(),
				goals.vocabulary(), goals.outputSchemaName(), goals.outputSchemaVersion(), true, 3,
				DefinitionSource.BUNDLED, null, null, null));
		store.seedBundled(review("ai-requirements-review", Set.of()));
	}

	private AssistantDefinitionEntity keep(AssistantDefinitionEntity entity) {
		if (!rows.contains(entity)) {
			rows.add(entity);
		}
		return entity;
	}

	private static Draft draft(String kind, String key, Consumer<DraftBuilder> changes) {
		DraftBuilder b = new DraftBuilder();
		b.kind = kind;
		b.key = key;
		changes.accept(b);
		return new Draft(b.kind, b.key, b.displayName, b.scope, b.providers, Map.of(),
				b.instructions, b.vocabulary, b.localOnly, b.executorBean);
	}

	private static final class DraftBuilder {
		String kind;
		String key;
		String displayName = "House style";
		Set<String> scope = Set.of();
		List<String> providers = List.of("entity");
		String instructions = "Check the house style.";
		List<Vocabulary> vocabulary = List.of(new Vocabulary("RULE_BROKEN", "broken", null));
		boolean localOnly;
		String executorBean;
	}

	private static List<String> fields(Runnable write) {
		InvalidDefinitionException e = catchThrowableOfType(write::run,
				InvalidDefinitionException.class);
		assertThat(e).as("the write was refused").isNotNull();
		return e.fieldProblems().stream().map(Problem::field).toList();
	}

	@Test
	void aProjectPolicyIsCreatedForThatProjectOnly() {
		View created = store.create(PROJECT, draft("policy", "house-style", b -> {
		}), "ron");

		assertThat(created.kind()).isEqualTo("POLICY");
		assertThat(created.taskType()).isEqualTo(AssistantDefinition.POLICY_REVIEW);
		assertThat(created.source()).isEqualTo("PROJECT");
		assertThat(created.version()).isEqualTo(1);
		assertThat(created.lockVersion()).isEqualTo(1);
		assertThat(created.bundledVersion()).isNull();
		// a policy with no scope applies to every reviewable type
		assertThat(created.inEffectFor()).containsExactlyInAnyOrderElementsOf(
				AssistantDefinitionValidator.REVIEWABLE_TYPES);
		assertThat(store.definitionsFor(PROJECT, AssistantDefinition.POLICY_REVIEW))
				.extracting(AssistantDefinition::key).containsExactly("house-style");
		assertThat(store.definitionsFor(OTHER, AssistantDefinition.POLICY_REVIEW)).isEmpty();
		assertThat(rows.get(rows.size() - 1).getCreatedBy()).isEqualTo("ron");
	}

	@Test
	void aNewDefinitionNeedsAGoodFreeKeyAKindAndRoom() {
		@SuppressWarnings("unchecked")
		ObjectProvider<RequelAssistant<?>> beans = mock(ObjectProvider.class);
		RequelAssistant<?> lexical = mock(RequelAssistant.class);
		when(lexical.assistantId()).thenReturn("legacy-lexical");
		doAnswer(inv -> {
			Consumer<RequelAssistant<?>> each = inv.getArgument(0);
			each.accept(lexical);
			return null;
		}).when(beans).forEach(any());
		store.setBeanAssistants(beans);

		assertThat(fields(() -> store.create(PROJECT, draft("POLICY", "House Style", b -> {
		}), "ron"))).containsExactly("key");
		assertThat(fields(() -> store.create(PROJECT, draft("POLICY", "legacy-lexical", b -> {
		}), "ron"))).containsExactly("key");
		assertThat(fields(() -> store.create(PROJECT, draft("REVIEW", "ai-review-goal", b -> {
		}), "ron"))).contains("key");
		assertThat(fields(() -> store.create(PROJECT, draft("SUMMARY", "house-style", b -> {
		}), "ron"))).contains("kind");
		assertThat(fields(() -> store.create(PROJECT, draft("POLICY", "house-style",
				b -> b.executorBean = "commandHandler"), "ron"))).containsExactly("executorBean");
		assertThat(fields(() -> store.create(PROJECT, draft("POLICY", "house-style", b -> {
			b.vocabulary = List.of();
			b.providers = List.of("telepathy");
			b.instructions = "x".repeat(64001);
		}), "ron"))).contains("vocabulary", "contextProviders", "instructions");
		assertThat(fields(() -> store.create(PROJECT, draft("POLICY", "house-style",
				b -> b.vocabulary = java.util.Arrays.asList((Vocabulary) null)), "ron")))
				.containsExactly("vocabulary");

		store.create(PROJECT, draft("POLICY", "house-style", b -> {
		}), "ron");
		assertThat(fields(() -> store.create(PROJECT, draft("POLICY", "house-style", b -> {
		}), "ron"))).containsExactly("key");
		store.setMaxPerProject(1);
		assertThat(fields(() -> store.create(PROJECT, draft("POLICY", "second", b -> {
		}), "ron"))).containsExactly("key");
		assertThat(store.definitionsFor(PROJECT, AssistantDefinition.POLICY_REVIEW)).hasSize(1);
	}

	@Test
	void aForkRecordsItsOriginAndAnEditBumpsTheVersion() {
		View fork = store.fork(PROJECT, "ai-review-goal", "ron");

		assertThat(fork.source()).isEqualTo("PROJECT");
		assertThat(fork.forkedFromVersion()).isEqualTo(3);
		assertThat(fork.bundledVersion()).isEqualTo(3);
		assertThat(fork.version()).isEqualTo(1);
		assertThat(fork.inEffectFor()).containsExactly("Goal");

		View edited = store.edit(PROJECT, "ai-review-goal", fork.lockVersion(),
				draft(null, "ai-review-goal", b -> {
					b.displayName = "Our goal review";
					b.scope = Set.of("Goal");
					b.instructions = "Goals name a measurable outcome.";
				}), "ana");

		assertThat(edited.version()).isEqualTo(2);
		assertThat(edited.forkedFromVersion()).isEqualTo(3);
		assertThat(edited.lockVersion()).isEqualTo(fork.lockVersion() + 1);
		assertThat(store.definitionsFor(PROJECT, TASK)).filteredOn(d -> d.key().equals(
				"ai-review-goal")).singleElement().satisfies(d -> {
					assertThat(d.instructions()).isEqualTo("Goals name a measurable outcome.");
					assertThat(d.source()).isEqualTo(DefinitionSource.PROJECT);
				});
		// the other project still runs the bundled one
		assertThat(store.definitionsFor(OTHER, TASK)).filteredOn(d -> d.key().equals(
				"ai-review-goal")).singleElement().extracting(AssistantDefinition::source)
				.isEqualTo(DefinitionSource.BUNDLED);
	}

	@Test
	void aStaleOrIllegalEditIsRefused() {
		View fork = store.fork(PROJECT, "ai-review-goal", "ron");

		assertThatThrownBy(() -> store.edit(PROJECT, "ai-review-goal", fork.lockVersion() - 1,
				draft(null, "ai-review-goal", b -> b.scope = Set.of("Goal")), "ana"))
				.isInstanceOf(EntityLockException.class);
		assertThat(fields(() -> store.edit(PROJECT, "ai-review-goal", fork.lockVersion(),
				draft("POLICY", "ai-review-goal", b -> b.scope = Set.of("Goal")), "ana")))
				.contains("kind");
		assertThat(fields(() -> store.edit(PROJECT, "ai-review-goal", fork.lockVersion(),
				draft(null, "ai-review-goal", b -> {
					b.scope = Set.of("Goal");
					b.executorBean = "x";
				}), "ana"))).containsExactly("executorBean");
		assertThat(fields(() -> store.edit(PROJECT, "nobody", 0, draft(null, "nobody", b -> {
		}), "ana"))).containsExactly("key");
		InvalidDefinitionException notForked = catchThrowableOfType(() -> store.edit(OTHER,
				"ai-review-goal", 0, draft(null, "ai-review-goal", b -> {
				}), "ana"), InvalidDefinitionException.class);
		assertThat(notForked).hasMessageContaining("customize the bundled one first");
		assertThat(fields(() -> store.fork(PROJECT, "ai-review-goal", "ron")))
				.containsExactly("key");
		assertThat(fields(() -> store.fork(PROJECT, "nothing", "ron"))).containsExactly("key");
	}

	@Test
	void revertBringsTheBundledDefinitionBackAndDeleteRemovesTheProjectsOwn() {
		View fork = store.fork(PROJECT, "ai-review-goal", "ron");
		View own = store.create(PROJECT, draft("POLICY", "house-style", b -> {
		}), "ron");

		assertThat(fields(() -> store.revert(PROJECT, "house-style", own.lockVersion())))
				.containsExactly("key");
		assertThat(fields(() -> store.delete(PROJECT, "ai-review-goal", fork.lockVersion())))
				.containsExactly("key");
		assertThatThrownBy(() -> store.revert(PROJECT, "ai-review-goal", fork.lockVersion() + 1))
				.isInstanceOf(EntityLockException.class);
		assertThatThrownBy(() -> store.delete(PROJECT, "house-style", own.lockVersion() + 1))
				.isInstanceOf(EntityLockException.class);

		store.revert(PROJECT, "ai-review-goal", fork.lockVersion());
		store.delete(PROJECT, "house-style", own.lockVersion());

		assertThat(store.definitionsFor(PROJECT, TASK)).filteredOn(d -> d.key().equals(
				"ai-review-goal")).singleElement().satisfies(d -> {
					assertThat(d.source()).isEqualTo(DefinitionSource.BUNDLED);
					assertThat(d.version()).isEqualTo(3);
				});
		assertThat(store.definitionsFor(PROJECT, AssistantDefinition.POLICY_REVIEW)).isEmpty();
		assertThat(rows).allMatch(r -> r.getProjectId() == null);
	}

	@Test
	void theEffectiveListShowsWhatRunsWhereAndWhichBundledVersionIsNewer() {
		store.fork(PROJECT, "ai-review-goal", "ron");
		// the bundled goal review moves on to version 4
		AssistantDefinition goals = review("ai-review-goal", Set.of("Goal"));
		store.seedBundled(new AssistantDefinition(goals.key(), "Goal review", goals.kind(),
				goals.taskType(), goals.scope(), goals.contextProviders(), goals.instructions(),
				goals.vocabulary(), goals.outputSchemaName(), goals.outputSchemaVersion(), true, 4,
				DefinitionSource.BUNDLED, null, null, null));

		List<View> effective = store.effective(PROJECT);

		assertThat(effective).extracting(View::key).containsExactly("ai-requirements-review",
				"ai-review-goal");
		View fallback = effective.get(0);
		assertThat(fallback.source()).isEqualTo("BUNDLED");
		assertThat(fallback.lockVersion()).isZero();
		// the goal review covers Goal, so the fallback covers the rest
		assertThat(fallback.inEffectFor()).doesNotContain("Goal").contains("Story", "Step");
		View fork = effective.get(1);
		assertThat(fork.forkedFromVersion()).isEqualTo(3);
		assertThat(fork.bundledVersion()).isEqualTo(4);
		assertThat(store.find(PROJECT, "ai-review-goal")).contains(fork);
		assertThat(store.find(PROJECT, "nothing")).isEmpty();
		assertThat(store.bundled("ai-review-goal")).hasValueSatisfying(b -> {
			assertThat(b.source()).isEqualTo("BUNDLED");
			assertThat(b.version()).isEqualTo(4);
		});
		assertThat(store.bundled("nothing")).isEmpty();
	}

	@Test
	void aCorpusFallbackCoversTheSetKindsNoSpecificOneDoes() {
		AssistantDefinition corpus = new AssistantDefinition("c-any", "Corpus",
				DefinitionKind.CORPUS, AssistantDefinition.CORPUS_REVIEW, Set.of(),
				List.of("corpus-index"), "x", List.of(new VocabularyEntry("OTHER", "o")),
				AssistantDefinition.CORPUS_OUTPUT_SCHEMA, "1", true, 1, DefinitionSource.BUNDLED,
				null, null, null);
		AssistantDefinition goalCorpus = new AssistantDefinition("c-goal", "Goal corpus",
				DefinitionKind.CORPUS, AssistantDefinition.CORPUS_REVIEW, Set.of("GOAL"),
				List.of("corpus-index"), "x", List.of(new VocabularyEntry("OTHER", "o")),
				AssistantDefinition.CORPUS_OUTPUT_SCHEMA, "1", true, 1, DefinitionSource.PROJECT,
				PROJECT, null, null);

		assertThat(AssistantDefinitionStore.inEffectFor(corpus, List.of(corpus, goalCorpus)))
				.containsExactly("PROJECT", "USE_CASE");
		assertThat(AssistantDefinitionStore.inEffectFor(goalCorpus, List.of(corpus, goalCorpus)))
				.containsExactly("GOAL");
		assertThat(AssistantDefinitionStore.taskType(DefinitionKind.CORPUS))
				.isEqualTo(AssistantDefinition.CORPUS_REVIEW);
	}
}
