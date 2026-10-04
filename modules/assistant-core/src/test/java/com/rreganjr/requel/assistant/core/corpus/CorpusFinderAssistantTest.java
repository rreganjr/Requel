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
package com.rreganjr.requel.assistant.core.corpus;

import static com.rreganjr.requel.assistant.core.corpus.CorpusFixture.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.CommandBackedAssistantResultApplicator;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Hint;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.HintType;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Kind;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Member;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.ScoredPair;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Settings;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.SwitchableAssistantCatalog;
import com.rreganjr.requel.project.UseCase;

/** Issue #266, stage 1: "Find overlaps" over a mocked project. */
class CorpusFinderAssistantTest {

	private static final EntityRef G1 = EntityRef.of("Goal", 1L);
	private static final EntityRef G2 = EntityRef.of("Goal", 2L);
	private static final EntityRef G3 = EntityRef.of("Goal", 3L);
	private static final EntityRef G4 = EntityRef.of("Goal", 4L);

	private final CorpusFixture fixture = new CorpusFixture();
	private Goal goal1;
	private Project project;
	private CorpusFinderAssistant assistant;

	@BeforeEach
	void setUp() {
		goal1 = fixture.goal(1, "Borrow limit", "Members may borrow 5 books at a time");
		fixture.goal(2, "Loan cap", "Members may borrow 10 books at a time");
		fixture.goal(3, "Reserve books online",
				"Members reserve books online and collect them at the desk");
		fixture.goal(4, "Online reservations",
				"Members reserve books online and collect them at the front desk");
		fixture.goal(5, "Host events", "The library hosts monthly reading events for children");
		project = fixture.project(7);
		assistant = new CorpusFinderAssistant(provider(null), 0.35, 0.20, 0.5);
	}

	/** A provider of {@code relations}, or of nothing when null. */
	@SuppressWarnings("unchecked")
	private static ObjectProvider<WordRelations> provider(WordRelations relations) {
		ObjectProvider<WordRelations> provider = mock(ObjectProvider.class);
		doAnswer(invocation -> relations != null ? relations
				: ((Supplier<WordRelations>) invocation.getArgument(0)).get())
				.when(provider).getIfAvailable(any(Supplier.class));
		return provider;
	}

	private static AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(1L, "ron"),
				new UserRef(1L, "assistant"), EntityRef.of("Project", 7L), CorpusFinderAssistant.TASK_TYPE,
				Locale.ROOT, Clock.systemUTC(), Map.of());
	}

	private static List<AnnotationAction> on(AssistantResult result, EntityRef ref) {
		return result.annotationActions().stream().filter(a -> a.targetRef().equals(ref)).toList();
	}

	@Test
	void describesItself() {
		assertThat(assistant.assistantId()).isEqualTo("corpus-finder");
		assertThat(assistant.targetType()).isEqualTo(Object.class);
		assertThat(assistant.handlesTask(CorpusFinderAssistant.TASK_TYPE)).isTrue();
		assertThat(assistant.handlesTask(null)).isFalse();
		assertThat(assistant.handlesTask("REVIEW")).isFalse();
		assertThat(assistant.cleanupPolicy()).isEqualTo(CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED);
		assertThat(assistant.displayName()).isEqualTo("Find overlaps");
		assertThat(assistant.group()).isEqualTo(SwitchableAssistantCatalog.CORPUS);
		assertThat(assistant.settings()).isEqualTo(new Settings(0.35, 0.20, 0.5));
	}

	@Test
	void aProjectRunRaisesAConflictAndAnOverlapOnBothSides() throws AssistantException {
		AssistantContext context = context();
		AssistantResult result = assistant.analyze(context, project);

		assertThat(result.assistantId()).isEqualTo("corpus-finder");
		assertThat(result.runId()).isEqualTo(context.runId());
		assertThat(result.summary()).isEqualTo("5 entities; 1 possible conflicts, 1 possible overlaps");
		assertThat(result.metadata()).containsEntry("members", 5).containsEntry("candidates", 2)
				.containsEntry(CommandBackedAssistantResultApplicator.CORPUS_SCOPE,
						List.of("Goal:1", "Goal:2", "Goal:3", "Goal:4", "Goal:5"));

		assertThat(result.annotationActions()).hasSize(4);
		AnnotationAction conflict = on(result, G1).get(0);
		assertThat(conflict.text()).startsWith("Possible conflict between Goal \"Borrow limit\" and"
				+ " Goal \"Loan cap\": ").contains("'5").endsWith(". Check that both can hold.");
		assertThat(conflict.severity()).isEqualTo("LOW");
		assertThat(conflict.actionKey()).startsWith("corpus-finder:Goal:1:POSSIBLE_CONFLICT-");
		assertThat(on(result, G2).get(0).text()).isEqualTo(conflict.text());

		AnnotationAction overlap = on(result, G3).get(0);
		assertThat(overlap.text()).startsWith("Possible overlap between Goal \"Reserve books"
				+ " online\" and Goal \"Online reservations\" (similarity ")
				.endsWith("). Check whether one repeats or refines the other.");
		assertThat(overlap.actionKey()).contains(":POSSIBLE_OVERLAP-");
		assertThat(on(result, G4)).hasSize(1);
		assertThat(on(result, EntityRef.of("Goal", 5L))).isEmpty();
	}

	@Test
	void aGoalRunCoversOnlyTheGoalSet() throws AssistantException {
		AssistantResult result = assistant.analyze(context(), goal1);

		assertThat(result.summary()).isEqualTo("1 entities; 0 possible conflicts, 0 possible overlaps");
		assertThat(result.annotationActions()).isEmpty();
		assertThat(result.metadata()).containsEntry(CommandBackedAssistantResultApplicator.CORPUS_SCOPE,
				List.of("Goal:1"));
	}

	@Test
	void aUseCaseRunUsesTheDictionaryWhenThereIsOne() throws AssistantException {
		UseCase useCase = fixture.useCase(40, "Borrow a book", "A member borrows a book");
		Scenario scenario = fixture.scenario(50, "Borrow",
				step(60, "The member may borrow 5 books"), step(61, "The member may borrow 10 books"));
		when(useCase.getScenario()).thenReturn(scenario);
		fixture.project(7);
		CorpusFinderAssistant withDictionary = new CorpusFinderAssistant(
				provider(WordRelations.NONE), 0.35, 0.20, 0.5);

		AssistantResult result = withDictionary.analyze(context(), useCase);

		assertThat(result.metadata()).containsEntry("members", 4);
		// two steps of one scenario are linked: their conflict is still raised
		assertThat(on(result, EntityRef.of("Step", 60L))).singleElement()
				.extracting(AnnotationAction::text).asString().startsWith("Possible conflict");
	}

	@Test
	void onlyAProjectOrAGoalOrUseCaseOfAProjectIsATarget() {
		assertThatThrownBy(() -> assistant.analyze(context(), step(1, "x")))
				.isInstanceOf(AssistantException.class)
				.hasMessageContaining("not Step");
		Goal domainGoal = CorpusFixture.entity(Goal.class, 9L, "Shared", "In a domain");
		when(domainGoal.getProjectOrDomain()).thenReturn(mock(ProjectOrDomain.class));
		assertThatThrownBy(() -> assistant.analyze(context(), domainGoal))
				.isInstanceOf(AssistantException.class);
		UseCase orphan = CorpusFixture.entity(UseCase.class, 9L, "Orphan", "No project");
		assertThatThrownBy(() -> assistant.analyze(context(), orphan))
				.isInstanceOf(AssistantException.class);
	}

	@Test
	void theTextNamesEachHintOfAConflict() {
		ScoredPair pair = new ScoredPair(G1, G2, 0.3, 0.3, List.of(
				new Hint(HintType.NUMBER, "5 books", "10 books"),
				new Hint(HintType.NEGATION, "may", "may not")), Kind.CONFLICT, false);

		assertThat(CorpusFinderAssistant.text(pair, new Member(G1, "A", null),
				new Member(G2, null, null))).isEqualTo("Possible conflict between Goal \"A\" and"
						+ " Goal \"#2\": '5 books' / '10 books'; 'may' / 'may not'. Check that both"
						+ " can hold.");
		ScoredPair overlap = new ScoredPair(G1, G2, 0.456, 0.4, List.of(), Kind.OVERLAP, false);
		assertThat(CorpusFinderAssistant.text(overlap, new Member(G1, "A", null),
				new Member(G2, "B", null))).contains("(similarity 0.46)");
	}

	@Test
	void aRootIsReferencedByItsType() {
		assertThat(CorpusFinderAssistant.rootRef(project)).isEqualTo(EntityRef.of("Project", 7L));
		assertThat(CorpusFinderAssistant.rootRef(goal1)).isEqualTo(G1);
		assertThat(CorpusFinderAssistant.rootRef("x")).isNull();
	}
}
