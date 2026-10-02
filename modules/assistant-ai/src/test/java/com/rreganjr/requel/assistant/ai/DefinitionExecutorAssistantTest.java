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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.node.NullNode;

import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.core.context.ContextPackMetadata;
import com.rreganjr.requel.assistant.core.context.ContextSection;
import com.rreganjr.requel.assistant.core.context.EntityContextPack;
import com.rreganjr.requel.assistant.core.context.GoalSnapshot;
import com.rreganjr.requel.assistant.core.context.PackSpec;
import com.rreganjr.requel.assistant.core.context.RelatedEntity;
import com.rreganjr.requel.assistant.core.context.EntityContextPackBuilder;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinitionValidator;
import com.rreganjr.requel.assistant.core.definition.BundledDefinitions;
import com.rreganjr.requel.assistant.core.definition.DefinitionSource;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageRepository;
import com.rreganjr.requel.project.TextEntity;

class DefinitionExecutorAssistantTest {

	/** The pre-#260 hard-coded prompt, kept to pin the seeded default (the golden test). */
	static final String FORMER_TASK_INSTRUCTIONS = """
			You are a Requel requirements-analysis assistant performing a REQUIREMENTS_REVIEW.
			Analyze ONLY the target requirement entity described in the context and return JSON
			matching the supplied schema.

			Finding types (choose the closest): AMBIGUOUS, INCOMPLETE, UNTESTABLE, INCONSISTENT,
			REDUNDANT, UNCLEAR_ACTOR, MISSING_PRECONDITION, MISSING_ERROR_CASE, OTHER.

			Rules:
			- Do your own original analysis of the target entity's wording. The context may include
			  an "annotations" list of EXISTING issues/notes already on the entity; these are for
			  awareness only. Do NOT restate, echo, paraphrase, or duplicate them. Report only
			  genuinely new problems you find in the requirement text itself.
			- For EVERY finding you raise you MUST set "suggestedIssueText" to a clear, specific
			  problem statement about THIS entity. Optionally add "suggestedPositions" with concrete
			  ways to resolve it. Use "suggestedNoteText" only for a non-blocking observation.
			- If the requirement has no real problems, return an empty "findings" array. Never
			  invent issues to fill space.
			- Keep every finding specific to this entity and grounded in its actual text; cite the
			  relevant snippet in "evidenceReferences".

			Example of a single well-formed finding:
			{"findingType":"AMBIGUOUS","severity":"MEDIUM","confidence":0.7,
			 "evidenceReferences":["the system should be fast"],
			 "suggestedIssueText":"'fast' is not measurable; specify a target response time.",
			 "suggestedNoteText":null,
			 "suggestedPositions":["Define a concrete latency budget, e.g. 200ms p95."]}
			""";


	private final EntityContextPackBuilder packBuilder = mock(EntityContextPackBuilder.class);
	private final RecordingAiClient aiClient = new RecordingAiClient();
	private final AssistantUsageRepository usageRepository = mock(AssistantUsageRepository.class);
	private final ObjectMapper objectMapper = new ObjectMapper();
	private AiDefinitionExecutorFactory factory;

	@Test
	void handlesOnlyRequirementsReviewTask() {
		DefinitionExecutorAssistant assistant = newAssistant(enabledProperties());
		assertThat(assistant.handlesTask("REQUIREMENTS_REVIEW")).isTrue();
		assertThat(assistant.handlesTask(null)).isFalse();
		assertThat(assistant.handlesTask("SOMETHING_ELSE")).isFalse();
	}

	@Test
	void skipsWhenNotRequirementsReviewTask() throws Exception {
		AssistantResult result = newAssistant(enabledProperties())
				.analyze(context(null), goalTarget());

		assertThat(aiClient.calls).isZero();
		assertThat(result.annotationActions()).isEmpty();
	}

	@Test
	void skipsWhenAiDisabled() throws Exception {
		AiProperties disabled = new AiProperties(); // enabled defaults to false
		AssistantResult result = newAssistant(disabled)
				.analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		assertThat(aiClient.calls).isZero();
		assertThat(result.annotationActions()).isEmpty();
	}

	@Test
	void skipsWhenProjectNotAllowed() throws Exception {
		AiProperties props = enabledProperties();
		props.setProjectAllowlist(List.of("999")); // run's project id is 2
		AssistantResult result = newAssistant(props)
				.analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		assertThat(aiClient.calls).isZero();
		assertThat(result.annotationActions()).isEmpty();
	}

	@Test
	void callsProviderWhenRequirementsReviewEnabledAndAllowed() throws Exception {
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));

		AssistantResult result = newAssistant(enabledProperties())
				.analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		assertThat(aiClient.calls).isEqualTo(1);
		assertThat(aiClient.lastRequest.taskType()).isEqualTo("REQUIREMENTS_REVIEW");
		assertThat(aiClient.lastRequest.assistantId()).isEqualTo("ai-requirements-review");
		// The request carries the loaded REQUIREMENTS_REVIEW output schema.
		assertThat(aiClient.lastRequest.outputSchemaName()).isEqualTo("RequirementsReviewOutput");
		assertThat(aiClient.lastRequest.outputSchema().path("properties").has("findings")).isTrue();
		// The centralized task guidance is carried on the request (not duplicated per provider).
		assertThat(aiClient.lastRequest.instructions())
				.contains("REQUIREMENTS_REVIEW", "suggestedIssueText", "NOT restate");
		assertThat(result.summary()).isEqualTo("noop summary");
		assertThat(result.annotationActions()).isEmpty(); // the canned response has no findings
		verify(usageRepository).save(any()); // usage telemetry is persisted on a successful call
	}

	@Test
	void skipsProviderWhenContextExceedsInputCap() throws Exception {
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));
		AiProperties props = enabledProperties();
		props.setMaxInputTokens(-1); // force any estimate to exceed the cap

		AssistantResult result = newAssistant(props)
				.analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		assertThat(aiClient.calls).isZero();
		assertThat(result.annotationActions()).isEmpty();
		assertThat(result.messages()).anyMatch(m -> m.text().contains("maxInputTokens"));
	}

	@Test
	void theBundledReviewAsksForTheBasePackOnly() throws Exception {
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));

		newAssistant(enabledProperties()).analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		ArgumentCaptor<PackSpec> spec = ArgumentCaptor.forClass(PackSpec.class);
		verify(packBuilder).build(any(), spec.capture());
		assertThat(spec.getValue().providerIds()).containsExactly("entity");
		assertThat(spec.getValue().maxCharacters()).isZero(); // the pack's own cap, as before
	}

	@Test
	void aDefinitionWithProvidersCapsThePackAtTheInputBudget() throws Exception {
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));
		AssistantDefinition d = defaultDefinition();
		AssistantDefinition withProviders = new AssistantDefinition(d.key(), d.displayName(),
				d.kind(), d.taskType(), d.scope(), List.of("entity", "goal-relations"),
				d.instructions(), d.vocabulary(), d.outputSchemaName(), d.outputSchemaVersion(),
				true, 1, d.source(), null, null, null, Map.of("goal-relations", 500));

		newAssistant(enabledProperties(), withProviders).analyze(context("REQUIREMENTS_REVIEW"),
				goalTarget());

		ArgumentCaptor<PackSpec> spec = ArgumentCaptor.forClass(PackSpec.class);
		verify(packBuilder).build(any(), spec.capture());
		assertThat(spec.getValue().providerIds()).containsExactly("entity", "goal-relations");
		assertThat(spec.getValue().budgets()).containsEntry("goal-relations", 500);
		assertThat(spec.getValue().maxCharacters()).isEqualTo(16000 * 4);
	}

	@Test
	void anOversizeProviderSectionIsDroppedRatherThanTheReviewSkipped() throws Exception {
		ContextSection big = ContextSection.complete("goal-siblings", List.of(new RelatedEntity(
				EntityRef.of("Goal", 11L), "sibling goal", "Other", "x".repeat(20_000))));
		EntityContextPack pack = new EntityContextPack(EntityRef.of("Goal", 10L),
				new GoalSnapshot(10L, 1, "Members love the library", "enjoy it"), null, null,
				null, null, List.of(big), ContextPackMetadata.empty(java.time.Instant.EPOCH));
		when(packBuilder.build(any(), any())).thenReturn(pack);
		AiProperties props = enabledProperties();
		props.setMaxInputTokens(1000); // the base pack fits; with the section it does not
		AiDefinitionExecutorFactory sized = new AiDefinitionExecutorFactory(aiClient, packBuilder,
				props, usageRepository, com.fasterxml.jackson.databind.json.JsonMapper.builder()
						.disable(com.fasterxml.jackson.databind.MapperFeature
								.REQUIRE_HANDLERS_FOR_JAVA8_TIMES)
						.build());

		((DefinitionExecutorAssistant) sized.executorFor(defaultDefinition())).analyze(context("REQUIREMENTS_REVIEW"),
				goalTarget());

		assertThat(aiClient.calls).isEqualTo(1);
		EntityContextPack sent = (EntityContextPack) aiClient.lastRequest.contextPacks().get(0);
		assertThat(sent.context()).isEmpty();
		assertThat(sent.metadata().truncationNotes())
				.contains("goal-siblings: dropped to fit the input cap");
	}

	@Test
	void providerFailurePropagatesSoTheRunRecordsIt() {
		// #259: an AiAnalysisException used to be swallowed into a result, so the run read as a
		// successful review that found nothing.
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));
		aiClient.failure = new AiAnalysisException("AI CLI exited with status 2");

		assertThatThrownBy(() -> newAssistant(enabledProperties())
				.analyze(context("REQUIREMENTS_REVIEW"), goalTarget()))
				.isInstanceOf(AssistantException.class)
				.hasMessageContaining("AI CLI exited with status 2")
				.hasCauseInstanceOf(AiAnalysisException.class);
		verify(usageRepository, never()).save(any()); // no usage row for a failed call
	}

	/** #262: every request carries the project's data-handling flags. */
	@Test
	void theRequestCarriesTheProjectsDataHandlingFlags() throws Exception {
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));
		com.rreganjr.requel.project.ProjectAssistantSettingsStore store =
				mock(com.rreganjr.requel.project.ProjectAssistantSettingsStore.class);
		when(store.disabledAssistants(2L)).thenReturn(java.util.Set.of("egress.external",
				"redaction.phone"));
		DefinitionExecutorAssistant assistant = newAssistant(enabledProperties());
		factory.setSettingsStore(store);
		factory.setProviderLocality(AiProviderLocality.REMOTE);

		assistant.analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		assertThat(aiClient.lastRequest.dataHandlingFlags())
				.containsEntry("externalProviderAllowed", false)
				.containsEntry("providerLocality", "remote")
				.containsEntry("provider", enabledProperties().getProvider())
				.containsEntry("redactionCategories", List.of("credentials", "email", "ssn", "card"));
	}

	@Test
	void withoutSettingsEverythingIsOnAndTheFlagsAreNeverEmpty() throws Exception {
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));

		newAssistant(enabledProperties()).analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		assertThat(aiClient.lastRequest.dataHandlingFlags())
				.containsEntry("externalProviderAllowed", true)
				.containsKeys("providerLocality", "provider", "redactionCategories");
	}

	/** #262: the pack's redactions are recorded on the run before the provider is called. */
	@Test
	void redactionsAreRecordedOnTheRun() throws Exception {
		EntityContextPack pack = mock(EntityContextPack.class);
		when(pack.metadata()).thenReturn(new com.rreganjr.requel.assistant.core.context
				.ContextPackMetadata(java.time.Instant.EPOCH, 0, false,
						List.of("goal.text: CREDENTIALS x1, EMAIL x1"), List.of()));
		when(packBuilder.build(any(), any())).thenReturn(pack);
		com.rreganjr.requel.assistant.core.InMemoryAssistantRunStore runStore =
				new com.rreganjr.requel.assistant.core.InMemoryAssistantRunStore();
		DefinitionExecutorAssistant assistant = newAssistant(enabledProperties());
		factory.setRunStore(runStore);
		AssistantContext context = context("REQUIREMENTS_REVIEW");

		assistant.analyze(context, goalTarget());

		assertThat(runStore.redactions(context.runId())).hasValueSatisfying(r -> {
			assertThat(r.getKey()).isEqualTo(2);
			assertThat(r.getValue()).containsExactly("CREDENTIALS", "EMAIL");
		});
	}

	@Test
	void mapsFindingsToAnnotationActions() throws Exception {
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));
		aiClient.response = new AiAnalysisResponse("found two issues", NullNode.getInstance(),
				List.of(
						new AiFindingDraft("AMBIGUOUS", "HIGH", 0.9, List.of("Goal:10"),
								"Requirement is ambiguous", null, List.of("Clarify the actor"),
								Map.of()),
						new AiFindingDraft("COMPLETENESS", null, null, List.of(), null,
								"Consider error cases", List.of(), Map.of())),
				List.of(), AiUsage.noop("noop", Duration.ZERO), Map.of());

		AssistantResult result = newAssistant(enabledProperties())
				.analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		List<AnnotationAction> actions = result.annotationActions();
		assertThat(actions).hasSize(3); // issue + its position + note

		AnnotationAction issue = actions.stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE)
				.findFirst().orElseThrow();
		assertThat(issue.text()).isEqualTo("Requirement is ambiguous");
		assertThat(issue.severity()).isEqualTo("HIGH");
		assertThat(issue.confidence()).isEqualTo(0.9);
		assertThat(issue.targetRef()).isEqualTo(EntityRef.of("TextEntity", 10L));

		AnnotationAction position = actions.stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION)
				.findFirst().orElseThrow();
		assertThat(position.parentActionKey()).isEqualTo(issue.actionKey());
		assertThat(position.text()).isEqualTo("Clarify the actor");

		AnnotationAction note = actions.stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_NOTE)
				.findFirst().orElseThrow();
		assertThat(note.text()).isEqualTo("Consider error cases");
	}

	/** #260 golden test: the seeded default carries the former hard-coded prompt exactly. */
	@Test
	void theSeededDefaultCarriesTheFormerPromptExactly() throws Exception {
		AssistantDefinition definition = defaultDefinition();
		assertThat(definition.instructions()).isEqualTo(FORMER_TASK_INSTRUCTIONS);
		assertThat(definition.key()).isEqualTo("ai-requirements-review");
		assertThat(definition.taskType()).isEqualTo("REQUIREMENTS_REVIEW");
		assertThat(definition.isFallback()).isTrue();
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));

		newAssistant(enabledProperties()).analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		assertThat(aiClient.lastRequest.instructions()).isEqualTo(FORMER_TASK_INSTRUCTIONS);
		assertThat(aiClient.lastRequest.assistantId()).isEqualTo("ai-requirements-review");
	}

	/** #260: the definition's own instructions, key and identity reach the request. */
	@Test
	void aDefinitionsInstructionsAndKeyReachTheRequest() throws Exception {
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));
		AssistantDefinition forked = defaultDefinition().forkFor(2L, "Project-specific guidance.");

		DefinitionExecutorAssistant assistant = newAssistant(enabledProperties(), forked);
		assistant.analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		assertThat(assistant.definition()).isSameAs(forked);
		assertThat(aiClient.lastRequest.instructions()).isEqualTo("Project-specific guidance.");
		assertThat(aiClient.lastRequest.attributes())
				.containsEntry("definitionKey", "ai-requirements-review")
				.containsEntry("definitionVersion", 1)
				.containsEntry("definitionSource", DefinitionSource.PROJECT.name());
	}

	/** #260: a finding citing text the entity doesn't contain is kept, and counted. */
	@Test
	void unverifiedEvidenceIsKeptAndCounted() throws Exception {
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));
		aiClient.response = new AiAnalysisResponse("two findings", NullNode.getInstance(),
				List.of(
						new AiFindingDraft("AMBIGUOUS", "HIGH", 0.9, List.of("\"Members love\""),
								"Real evidence", null, List.of(), Map.of()),
						new AiFindingDraft("INCOMPLETE", "LOW", 0.5,
								List.of("text the entity never says"), "Made-up evidence", null,
								List.of(), Map.of())),
				List.of(), AiUsage.noop("noop", Duration.ZERO), Map.of());

		AssistantResult result = newAssistant(enabledProperties())
				.analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		assertThat(result.annotationActions()).hasSize(2);
		assertThat(result.metadata()).containsEntry("evidenceUnverified", 1);
		assertThat(result.messages()).anyMatch(m -> m.text().contains("1 finding(s)"));
		AnnotationAction issue = result.annotationActions().get(0);
		assertThat(issue.metadata()).containsEntry("definitionKey", "ai-requirements-review");
	}

	@Test
	void evidenceChecking() {
		String entity = "Fast checkout\nThe system   should be\nfast.";
		assertThat(EvidenceCheck.unverified(List.of("the system should be fast"), entity)).isTrue();
		assertThat(EvidenceCheck.unverified(List.of("system should be fast"), entity)).isFalse();
		assertThat(EvidenceCheck.unverified(List.of("'Fast checkout'"), entity)).isFalse();
		assertThat(EvidenceCheck.unverified(List.of("“should be fast”"), entity)).isFalse();
		assertThat(EvidenceCheck.unverified(List.of(), entity)).isFalse();
		assertThat(EvidenceCheck.unverified(List.of(" "), entity)).isFalse();
		assertThat(EvidenceCheck.unverified(null, entity)).isFalse();
	}

	private DefinitionExecutorAssistant newAssistant(AiProperties properties) {
		return newAssistant(properties, defaultDefinition());
	}

	private DefinitionExecutorAssistant newAssistant(AiProperties properties,
			AssistantDefinition definition) {
		factory = new AiDefinitionExecutorFactory(aiClient, packBuilder, properties,
				usageRepository, objectMapper);
		return (DefinitionExecutorAssistant) factory.executorFor(definition);
	}

	/** The bundled default, read the way the seeder reads it. */
	private AssistantDefinition defaultDefinition() {
		List<AssistantDefinition> bundled = BundledDefinitions.load(objectMapper,
				new AssistantDefinitionValidator(16000));
		return bundled.stream().filter(d -> d.key().equals(RequirementsReview.ASSISTANT_ID))
				.findFirst().orElseThrow();
	}

	private static AiProperties enabledProperties() {
		AiProperties properties = new AiProperties();
		properties.setEnabled(true);
		return properties;
	}

	private static AssistantContext context(String taskType) {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "human"),
				new UserRef(4L, "assistant"), EntityRef.of("Project", 2L), taskType,
				java.util.Locale.ROOT, Clock.systemUTC(), Map.of());
	}

	private static TextEntity goalTarget() {
		TextEntity target = mock(TextEntity.class);
		when(target.getId()).thenReturn(10L);
		when(target.getName()).thenReturn("Members love the library");
		when(target.getText()).thenReturn("Members should really enjoy using it.");
		doReturn(TextEntity.class).when(target).getProjectOrDomainEntityInterface();
		return target;
	}

	/** Records invocations and returns a configurable response (no real provider). */
	private static final class RecordingAiClient implements AiAnalysisClient {
		private int calls;
		private AiAnalysisRequest lastRequest;
		private AiAnalysisResponse response = new AiAnalysisResponse("noop summary",
				NullNode.getInstance(), List.of(), List.of(), AiUsage.noop("noop", Duration.ZERO),
				Map.of());
		private AiAnalysisException failure;

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
