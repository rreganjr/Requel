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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Draft;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Kind;
import com.rreganjr.requel.assistant.ai.IdeaToRequirementsService.Idea;
import com.rreganjr.requel.assistant.ai.IdeaToRequirementsService.Outcome;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantMessage;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.context.EntityContextPackBuilder;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageRepository;

class IdeaToRequirementsServiceTest {

	private static final String IDEA_TEXT = "Let gym members book a class from their phone.";

	private final EntityContextPackBuilder packBuilder = mock(EntityContextPackBuilder.class);
	private final AssistantUsageRepository usageRepository = mock(AssistantUsageRepository.class);
	private final RecordingAiClient aiClient = new RecordingAiClient();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void sendsTheIdeaUnderTheIdeaPrompt() throws Exception {
		Idea idea = new Idea("  " + IDEA_TEXT + "  ", List.of("  Existing   goal ", " ", "Other"),
				null, List.of("Class"));

		Outcome outcome = service(enabledProperties()).analyze(context(IdeaToRequirements.TASK_TYPE),
				idea);

		assertThat(outcome.skipped()).isFalse();
		assertThat(aiClient.calls).isEqualTo(1);
		AiAnalysisRequest request = aiClient.lastRequest;
		assertThat(request.taskType()).isEqualTo(IdeaToRequirements.TASK_TYPE);
		assertThat(request.assistantId()).isEqualTo(IdeaToRequirements.ASSISTANT_ID);
		assertThat(request.outputSchemaName()).isEqualTo(IdeaToRequirements.OUTPUT_SCHEMA_NAME);
		assertThat(request.outputSchemaVersion()).isEqualTo(IdeaToRequirements.OUTPUT_SCHEMA_VERSION);
		assertThat(request.outputSchema().path("properties").has("findings")).isTrue();
		assertThat(request.targetRef()).isEqualTo(EntityRef.of("Project", 2L));
		assertThat(request.projectRef()).isEqualTo(EntityRef.of("Project", 2L));
		assertThat(request.dataHandlingFlags()).containsKeys("externalProviderAllowed",
				"providerLocality", "provider", "redactionCategories");
		assertThat(request.attributes()).containsEntry("definitionKey",
				IdeaToRequirements.ASSISTANT_ID);

		// the idea is the one context pack, stripped and with clean name lists
		assertThat(request.contextPacks()).hasSize(1);
		Idea sent = (Idea) request.contextPacks().get(0);
		assertThat(sent.ideaText()).isEqualTo(IDEA_TEXT);
		assertThat(sent.existingGoals()).containsExactly("Existing goal", "Other");
		assertThat(sent.existingActors()).isEmpty();
		assertThat(sent.existingGlossaryTerms()).containsExactly("Class");
		verify(usageRepository).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void theConfiguredMaximumIsInThePrompt() throws Exception {
		AiProperties properties = enabledProperties();
		properties.getIdea().setMaxProposals(3);

		service(properties).analyze(context(IdeaToRequirements.TASK_TYPE), idea());

		assertThat(aiClient.lastRequest.instructions()).contains("at most 3 entities")
				.doesNotContain(IdeaToRequirements.MAX_PLACEHOLDER);
	}

	@Test
	void skipsWhenNotAnIdeaRun() throws Exception {
		Outcome outcome = service(enabledProperties()).analyze(context("REQUIREMENTS_REVIEW"),
				idea());

		assertThat(outcome.skipped()).isTrue();
		assertThat(outcome.skippedReason()).contains(IdeaToRequirements.TASK_TYPE);
		assertThat(aiClient.calls).isZero();
	}

	@Test
	void skipsWhenAiIsDisabled() throws Exception {
		Outcome outcome = service(new AiProperties()).analyze(
				context(IdeaToRequirements.TASK_TYPE), idea());

		assertThat(outcome.skipped()).isTrue();
		assertThat(aiClient.calls).isZero();
	}

	@Test
	void skipsWhenTheProjectIsNotAllowed() throws Exception {
		AiProperties properties = enabledProperties();
		properties.setProjectAllowlist(List.of("999"));

		Outcome outcome = service(properties).analyze(context(IdeaToRequirements.TASK_TYPE),
				idea());

		assertThat(outcome.skipped()).isTrue();
		assertThat(aiClient.calls).isZero();
	}

	@Test
	void skipsWhenTheIdeaExceedsTheInputCap() throws Exception {
		AiProperties properties = enabledProperties();
		properties.setMaxInputTokens(-1); // any estimate exceeds it

		Outcome outcome = service(properties).analyze(context(IdeaToRequirements.TASK_TYPE),
				idea());

		assertThat(outcome.skipped()).isTrue();
		assertThat(outcome.skippedReason()).contains("maxInputTokens");
		assertThat(aiClient.calls).isZero();
	}

	@Test
	void rejectsABlankIdea() {
		IdeaToRequirementsService service = service(enabledProperties());
		AssistantContext context = context(IdeaToRequirements.TASK_TYPE);

		assertThatThrownBy(() -> service.analyze(context, new Idea("   ", null, null, null)))
				.isInstanceOf(AssistantException.class);
		assertThatThrownBy(() -> service.analyze(context, null))
				.isInstanceOf(AssistantException.class);
		assertThat(aiClient.calls).isZero();
	}

	@Test
	void rejectsAContextWithoutAProject() {
		AssistantContext noProject = new AssistantContext(UUID.randomUUID(), new UserRef(3L, "human"),
				new UserRef(4L, "assistant"), null, IdeaToRequirements.TASK_TYPE, Locale.ROOT,
				Clock.systemUTC(), Map.of());

		assertThatThrownBy(() -> service(enabledProperties()).analyze(noProject, idea()))
				.isInstanceOf(AssistantException.class);
		assertThat(aiClient.calls).isZero();
	}

	@Test
	void aProviderFailurePropagatesAndRecordsNoUsage() {
		aiClient.failure = new AiAnalysisException("provider exploded");

		assertThatThrownBy(() -> service(enabledProperties())
				.analyze(context(IdeaToRequirements.TASK_TYPE), idea()))
				.isInstanceOf(AssistantException.class)
				.hasMessageContaining("provider exploded")
				.hasCauseInstanceOf(AiAnalysisException.class);
		verify(usageRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void draftParsesAndValidatesTheReply() throws Exception {
		aiClient.response = new AiAnalysisResponse("Gym members book classes.",
				NullNode.getInstance(),
				List.of(
						finding("ACTOR", "Gym member", "A person who attends classes.",
								List.of("gym members"), List.of()),
						finding("GOAL", "Book remotely", "Members reserve a place without calling.",
								List.of("book a class from their phone"), List.of()),
						finding("USE_CASE", "Book a class", "A member books a class.",
								List.of("words the idea never said"),
								List.of("ACTOR:Gym member", "GOAL:Book remotely")),
						finding("SCREEN", "Booking screen", "Not an entity kind.", List.of(),
								List.of())),
				List.of(AssistantMessage.warning("What happens when a class is full?")),
				AiUsage.noop("noop", Duration.ZERO), Map.of());

		IdeaRequirementsDrafts drafts = service(enabledProperties())
				.draft(context(IdeaToRequirements.TASK_TYPE), idea());

		assertThat(drafts.skipped()).isFalse();
		assertThat(drafts.summary()).isEqualTo("Gym members book classes.");
		assertThat(drafts.drafts()).extracting(Draft::name)
				.containsExactly("Gym member", "Book remotely", "Book a class");
		assertThat(drafts.messages()).hasSize(1);

		Draft useCase = drafts.ofKind(Kind.USE_CASE).get(0);
		assertThat(useCase.references()).extracting(IdeaRequirementsDrafts.Reference::name)
				.containsExactly("Gym member", "Book remotely");
		assertThat(useCase.evidenceUnverified()).isTrue();
		assertThat(drafts.ofKind(Kind.ACTOR).get(0).evidenceUnverified()).isFalse();

		// the bad finding is dropped and reported, not thrown
		assertThat(drafts.problems()).anyMatch(
				p -> p.dropped() && p.subject().equals("Booking screen"));
	}

	@Test
	void draftOfASkippedCallCarriesTheReason() throws Exception {
		IdeaRequirementsDrafts drafts = service(new AiProperties())
				.draft(context(IdeaToRequirements.TASK_TYPE), idea());

		assertThat(drafts.skipped()).isTrue();
		assertThat(drafts.skippedReason()).contains("disabled");
		assertThat(drafts.drafts()).isEmpty();
		assertThat(aiClient.calls).isZero();
	}

	private IdeaToRequirementsService service(AiProperties properties) {
		return new IdeaToRequirementsService(new AiDefinitionExecutorFactory(aiClient, packBuilder,
				properties, usageRepository, objectMapper));
	}

	private static Idea idea() {
		return new Idea(IDEA_TEXT, List.of(), List.of(), List.of());
	}

	private static AiFindingDraft finding(String type, String name, String text,
			List<String> evidence, List<String> participants) {
		return new AiFindingDraft(type, null, 0.8, evidence, text, null, List.of(),
				Map.of(ReviewResultMapper.SUGGESTED_ENTITY_NAME, name,
						ReviewResultMapper.PARTICIPANTS, participants));
	}

	private static AiProperties enabledProperties() {
		AiProperties properties = new AiProperties();
		properties.setEnabled(true);
		return properties;
	}

	private static AssistantContext context(String taskType) {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "human"),
				new UserRef(4L, "assistant"), EntityRef.of("Project", 2L), taskType, Locale.ROOT,
				Clock.systemUTC(), Map.of());
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
