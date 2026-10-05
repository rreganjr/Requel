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
package com.rreganjr.requel.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantMessage;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.AssistantRunStore;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.CommandBackedAssistantResultApplicator;
import com.rreganjr.requel.assistant.core.StagedAssistant;
import com.rreganjr.requel.assistant.core.context.ContextPackSizeLimits;
import com.rreganjr.requel.assistant.core.context.EntityContextPackBuilder;
import com.rreganjr.requel.assistant.core.context.RedactionPolicy;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Member;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Settings;
import com.rreganjr.requel.assistant.core.corpus.CorpusFinderAssistant;
import com.rreganjr.requel.assistant.core.corpus.CorpusMembers.CorpusSet;
import com.rreganjr.requel.assistant.core.corpus.CorpusPack;
import com.rreganjr.requel.assistant.core.corpus.CorpusPackBuilder;
import com.rreganjr.requel.assistant.core.corpus.RelationshipFindings;
import com.rreganjr.requel.assistant.core.corpus.WordRelations;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinitionValidator;
import com.rreganjr.requel.assistant.core.definition.BundledDefinitions;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageRepository;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.SwitchableAssistantCatalog;
import com.rreganjr.requel.project.TextEntity;
import com.rreganjr.requel.project.UseCase;

/**
 * Issue #266, stage 2: the AI corpus analysis over a mocked project - the request it sends, how
 * findings land on their participants, and its refusals.
 */
class CorpusAnalysisAssistantTest {

	private static final EntityRef G1 = EntityRef.of("Goal", 1L);
	private static final EntityRef G2 = EntityRef.of("Goal", 2L);
	private static final EntityRef G3 = EntityRef.of("Goal", 3L);

	/** Masks "secret" and notes it. */
	private static final RedactionPolicy MASK = (path, value, notes) -> {
		if (value.contains("secret")) {
			notes.add(path + " redacted");
			return value.replace("secret", "[masked]");
		}
		return value;
	};

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final AssistantDefinition CORPUS = BundledDefinitions
			.load(MAPPER, new AssistantDefinitionValidator(16000)).stream()
			.filter(d -> d.key().equals("ai-corpus-relationships")).findFirst().orElseThrow();

	private final RecordingAiClient aiClient = new RecordingAiClient();
	private final AssistantRunStore runStore = mock(AssistantRunStore.class);
	private AiProperties properties;
	private AiDefinitionExecutorFactory factory;
	private Project project;
	private Goal goal1;

	@BeforeEach
	void setUp() {
		properties = new AiProperties();
		properties.setEnabled(true);
		factory = new AiDefinitionExecutorFactory(aiClient, mock(EntityContextPackBuilder.class),
				properties, mock(AssistantUsageRepository.class), MAPPER);
		factory.setRunStore(runStore);
		factory.setCorpus(new CorpusPackBuilder(MASK, new ContextPackSizeLimits()), null, null);

		project = mock(Project.class);
		when(project.getId()).thenReturn(7L);
		goal1 = goal(1L, "Borrow limit", "Members may borrow 5 books at a time");
		Goal goal2 = goal(2L, "Loan cap", "Members may borrow 10 books at a time");
		Goal goal3 = goal(3L, "Host events", "The library hosts secret reading events");
		doReturn(new LinkedHashSet<>(List.of(goal1, goal2, goal3))).when(project).getGoals();
	}

	private <T extends TextEntity> T entity(Class<T> type, Long id, String name, String text) {
		T entity = mock(type);
		when(entity.getId()).thenReturn(id);
		when(entity.getName()).thenReturn(name);
		when(entity.getText()).thenReturn(text);
		doReturn(type).when(entity).getProjectOrDomainEntityInterface();
		return entity;
	}

	private Goal goal(Long id, String name, String text) {
		Goal goal = entity(Goal.class, id, name, text);
		when(goal.getProjectOrDomain()).thenReturn(project);
		return goal;
	}

	private CorpusAnalysisAssistant assistant() {
		return (CorpusAnalysisAssistant) factory.executorFor(CORPUS);
	}

	private static AssistantContext context(String taskType) {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "human"),
				new UserRef(4L, "assistant"), EntityRef.of("Project", 7L), taskType,
				java.util.Locale.ROOT, Clock.systemUTC(), Map.of());
	}

	private static AssistantContext context() {
		return context(AssistantDefinition.CORPUS_REVIEW);
	}

	private static AiFindingDraft finding(String type, Object participants, String text,
			List<String> evidence, List<String> positions) {
		Map<String, Object> metadata = participants == null ? Map.of()
				: Map.of(ReviewResultMapper.PARTICIPANTS, participants);
		return new AiFindingDraft(type, "HIGH", 0.9, evidence, text, null, positions, metadata);
	}

	private static List<AnnotationAction> of(AssistantResult result,
			AnnotationAction.ActionType type) {
		return result.annotationActions().stream().filter(a -> a.actionType() == type).toList();
	}

	private static List<String> warnings(AssistantResult result) {
		return result.messages().stream().map(AssistantMessage::text).toList();
	}

	@Test
	void describesItself() {
		CorpusAnalysisAssistant assistant = assistant();

		assertThat(assistant.definition()).isSameAs(CORPUS);
		assertThat(assistant.assistantId()).isEqualTo("ai-corpus-relationships");
		assertThat(assistant.displayName()).isEqualTo(CORPUS.displayName());
		assertThat(assistant.targetType()).isEqualTo(Object.class);
		assertThat(assistant.handlesTask(AssistantDefinition.CORPUS_REVIEW)).isTrue();
		assertThat(assistant.handlesTask(CorpusFinderAssistant.TASK_TYPE)).isFalse();
		assertThat(assistant.projectSwitchable()).isTrue();
		assertThat(assistant.group()).isEqualTo(SwitchableAssistantCatalog.CORPUS);
		assertThat(assistant.cleanupPolicy()).isEqualTo(CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED);
	}

	@Test
	void oneCallSendsThePackWithTheSetsMembersAsTheOnlyParticipants() throws Exception {
		AssistantContext context = context();

		AssistantResult result = assistant().analyze(context, project);

		assertThat(aiClient.calls).isEqualTo(1);
		AiAnalysisRequest request = aiClient.lastRequest;
		assertThat(request.taskType()).isEqualTo(AssistantDefinition.CORPUS_REVIEW);
		assertThat(request.targetRef()).isEqualTo(EntityRef.of("Project", 7L));
		assertThat(request.contextPacks()).singleElement().isInstanceOf(CorpusPack.class);
		CorpusPack pack = (CorpusPack) request.contextPacks().get(0);
		assertThat(pack.candidates()).hasSize(1);
		assertThat(request.outputSchema()
				.at("/properties/findings/items/properties/participants/items/enum"))
				.extracting(n -> n.asText()).containsExactly("Goal:1", "Goal:2", "Goal:3");
		// "secret" was masked before it left
		verify(runStore).recordRedactions(context.runId(), 1, List.of());

		assertThat(result.summary()).isEqualTo("noop summary");
		assertThat(result.annotationActions()).isEmpty();
		assertThat(result.metadata())
				.containsEntry(CommandBackedAssistantResultApplicator.CORPUS_SCOPE,
						List.of("Goal:1", "Goal:2", "Goal:3"))
				.containsEntry("members", 3).containsEntry("candidatesSent", 1)
				.containsEntry("partial", false);
		// the finder's issues on the judged pair retire, on both sides, whichever kind
		String conflict = RelationshipFindings.shareKey(CorpusFinderAssistant.POSSIBLE_CONFLICT,
				List.of(G1, G2));
		@SuppressWarnings("unchecked")
		List<String> retired = (List<String>) result.metadata()
				.get(CommandBackedAssistantResultApplicator.RETIRES_FINDINGS);
		assertThat(retired).hasSize(4)
				.contains(RelationshipFindings.actionKey("corpus-finder", G1, conflict),
						RelationshipFindings.actionKey("corpus-finder", G2, conflict));
	}

	/**
	 * #363: the set, the candidates and the pack are built in prepare; the stage makes the call
	 * and maps the findings without touching an entity or the run store.
	 */
	@Test
	void theStageTouchesNoEntity() throws Exception {
		aiClient.response = response(List.of(finding("CONTRADICTORY_REQUIREMENTS",
				List.of("Goal:1", "Goal:2"), "5 books or 10?", List.of("borrow 5 books"),
				List.of())), null);
		AssistantContext context = context();

		StagedAssistant.Stage stage = assistant().prepare(context, project);

		assertThat(aiClient.calls).isZero();
		verify(runStore).recordRedactions(context.runId(), 1, List.of());
		clearInvocations(project, goal1, runStore);

		List<AssistantResult> results = stage.complete();

		assertThat(aiClient.calls).isEqualTo(1);
		verifyNoInteractions(project, goal1, runStore);
		assertThat(of(results.get(0), AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE))
				.extracting(AnnotationAction::targetRef).containsExactly(G1, G2);
	}

	@Test
	void eachRelationshipIsOneIssueOnEveryParticipantRaisedOnce() throws Exception {
		aiClient.response = response(List.of(
				finding("CONTRADICTORY_REQUIREMENTS", List.of("Goal:1", " Goal:2 "),
						"5 books or 10?", List.of("borrow 5 books"), List.of("Keep 5", " ")),
				// the same relationship from the other side
				finding("CONTRADICTORY_REQUIREMENTS", List.of("Goal:2", "Goal:1"), "again",
						List.of(), List.of()),
				finding("NOT_A_TYPE", List.of("Goal:1", "Goal:3"), "off vocabulary",
						List.of("not in either text"), List.of()),
				finding("OTHER", List.of("Goal:1", "Goal:99"), "one unknown", List.of(),
						List.of()),
				finding("OTHER", null, "nobody named", List.of(), List.of()),
				finding("OTHER", List.of("Goal:2", "Goal:3"), null, List.of(), List.of())),
				List.of(AssistantMessage.info("from the model")));

		AssistantResult result = assistant().analyze(context(), project);

		List<AnnotationAction> issues = of(result,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE);
		assertThat(issues).extracting(AnnotationAction::targetRef).containsExactly(G1, G2, G1, G3);
		assertThat(issues.get(0).text()).isEqualTo("5 books or 10?");
		assertThat(issues.get(0).evidence()).isNotEmpty();
		assertThat(issues.get(0).confidence()).isEqualTo(0.9);
		assertThat(issues.get(0).metadata()).containsEntry("definitionKey",
				"ai-corpus-relationships");
		assertThat(of(result, AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION))
				.singleElement().satisfies(position -> {
					assertThat(position.text()).isEqualTo("Keep 5");
					assertThat(position.parentActionKey()).isEqualTo(issues.get(0).actionKey());
				});

		assertThat(result.metadata()).containsEntry(CorpusAnalysisAssistant.UNRESOLVED_PARTICIPANTS, 1)
				.containsEntry(AssistantRunWorker.VOCABULARY_MISSES, 1)
				.containsEntry(AssistantRunWorker.EVIDENCE_UNVERIFIED, 1);
		assertThat(warnings(result)).contains("from the model",
				"1 participant(s) not in the set; 3 finding(s) dropped for naming fewer than two",
				"1 finding(s) cite evidence that is not in the participants' text",
				"1 finding(s) use a type outside the definition's vocabulary");
	}

	@Test
	void aPartialPackSaysSoInTheSummaryAndAWarning() {
		CorpusSet set = new CorpusSet(List.of(new Member(G1, "a", "x"), new Member(G2, "b", "y")),
				java.util.Set.of(), Map.of(), Map.of());
		CorpusPack pack = new CorpusPack("PROJECT", "Project:7", List.of(), List.of(),
				new CorpusPack.Notes(2, 3, 1, true, 10, 20, List.of()));

		AssistantResult result = assistant().map(context(), set, pack, Map.of(), List.of(),
				response(List.of(), null));

		assertThat(result.summary()).isEqualTo("Partial: sent 1 of 3 candidate pairs. noop summary");
		assertThat(result.metadata()).containsEntry("partial", true)
				.containsEntry(CommandBackedAssistantResultApplicator.RETIRES_FINDINGS, List.of());
		assertThat(warnings(result)).containsExactly("Sent 1 of 3 candidate pairs; the rest were"
				+ " not analysed (requel.ai.corpus.max-input-chars)");
		assertThat(CorpusAnalysisAssistant.summary(null, new CorpusPack.Notes(0, 0, 0, false, 0, 0,
				List.of()))).isEmpty();
	}

	@Test
	void anIndexOverTheBudgetIsRefusedBeforeAnyCall() {
		properties.getCorpus().setMaxInputChars(10);
		UseCase useCase = entity(UseCase.class, 40L, "Borrow a book", "A member borrows a book");
		when(useCase.getProjectOrDomain()).thenReturn(project);
		doReturn(new LinkedHashSet<>(List.of(useCase))).when(project).getUseCases();

		assertThatThrownBy(() -> assistant().analyze(context(), useCase))
				.isInstanceOf(AssistantException.class)
				.hasMessageContaining("The corpus index for Use case 40 is")
				.hasMessageContaining("requel.ai.corpus.max-input-chars");
		assertThatThrownBy(() -> assistant().analyze(context(), goal1))
				.hasMessageContaining("The corpus index for Goal 1 is");
		assertThat(aiClient.calls).isZero();
	}

	@Test
	void onlyARootOfAProjectWithAPackBuilderIsAnalysed() {
		Goal inADomain = entity(Goal.class, 9L, "Shared", "In a domain");
		when(inADomain.getProjectOrDomain()).thenReturn(mock(ProjectOrDomain.class));

		assertThatThrownBy(() -> assistant().analyze(context(), "a string"))
				.isInstanceOf(AssistantException.class)
				.hasMessage("A corpus analysis runs on a project, a goal or a use case");
		assertThatThrownBy(() -> assistant().analyze(context(), inADomain))
				.isInstanceOf(AssistantException.class);
		factory.setCorpus(null, null, null);
		assertThatThrownBy(() -> assistant().analyze(context(), project))
				.hasMessageEndingWith("(no corpus pack builder)");
		assertThat(aiClient.calls).isZero();
	}

	@Test
	void anotherTaskOrAiSwitchedOffSkipsTheRun() throws Exception {
		AssistantResult otherTask = assistant().analyze(context("REQUIREMENTS_REVIEW"), project);
		properties.setEnabled(false);
		AssistantResult disabled = assistant().analyze(context(), project);

		assertThat(otherTask.summary()).startsWith("Not a CORPUS_REVIEW run");
		assertThat(disabled.summary()).contains("requel.ai.enabled=false");
		assertThat(aiClient.calls).isZero();
	}

	@Test
	void aProviderFailureFailsTheRunAndARedactionRecordFailureDoesNot() {
		aiClient.failure = new AiAnalysisException("provider down");
		doThrow(new IllegalStateException("db down")).when(runStore).recordRedactions(any(),
				anyInt(), anyList());

		assertThatThrownBy(() -> assistant().analyze(context(), project))
				.isInstanceOf(AssistantException.class)
				.hasMessage("AI corpus analysis failed: provider down");
		verify(runStore).recordRedactions(any(), eq(1), anyList());
	}

	@Test
	void theFactoryHandsOverTheDictionaryAndTheFinderSettings() {
		WordRelations dictionary = mock(WordRelations.class);
		@SuppressWarnings("unchecked")
		ObjectProvider<WordRelations> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable(any(java.util.function.Supplier.class))).thenReturn(dictionary);
		CorpusFinderAssistant finder = new CorpusFinderAssistant(provider, 0.4, 0.3, 0.2);

		factory.setCorpus(null, provider, finder);
		assertThat(factory.wordRelations()).isSameAs(dictionary);
		assertThat(factory.corpusFinderSettings()).isEqualTo(new Settings(0.4, 0.3, 0.2));

		factory.setCorpus(null, null, null);
		assertThat(factory.wordRelations()).isSameAs(WordRelations.NONE);
		assertThat(factory.corpusFinderSettings()).isEqualTo(Settings.DEFAULTS);
	}

	private static AiAnalysisResponse response(List<AiFindingDraft> findings,
			List<AssistantMessage> messages) {
		return new AiAnalysisResponse("noop summary", NullNode.getInstance(), findings,
				messages == null ? List.of() : messages, AiUsage.noop("noop", Duration.ZERO),
				Map.of());
	}

	/** Records invocations and returns a configurable response (no real provider). */
	private static final class RecordingAiClient implements AiAnalysisClient {
		private int calls;
		private AiAnalysisRequest lastRequest;
		private AiAnalysisException failure;
		private AiAnalysisResponse response = response(new ArrayList<>(), null);

		@Override
		public AiAnalysisResponse analyze(AiAnalysisRequest request) throws AiAnalysisException {
			this.calls++;
			this.lastRequest = request;
			if (failure != null) {
				throw failure;
			}
			return response;
		}
	}
}
