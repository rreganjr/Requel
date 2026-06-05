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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.NullNode;

import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.core.context.EntityContextPack;
import com.rreganjr.requel.assistant.core.context.EntityContextPackBuilder;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageRepository;
import com.rreganjr.requel.project.TextEntity;

class RequirementsReviewAssistantTest {

	private final EntityContextPackBuilder packBuilder = mock(EntityContextPackBuilder.class);
	private final RecordingAiClient aiClient = new RecordingAiClient();
	private final AssistantUsageRepository usageRepository = mock(AssistantUsageRepository.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void handlesOnlyRequirementsReviewTask() {
		RequirementsReviewAssistant assistant = newAssistant(enabledProperties());
		assertThat(assistant.handlesTask("REQUIREMENTS_REVIEW")).isTrue();
		assertThat(assistant.handlesTask(null)).isFalse();
		assertThat(assistant.handlesTask("SOMETHING_ELSE")).isFalse();
	}

	@Test
	void skipsWhenNotRequirementsReviewTask() {
		AssistantResult result = newAssistant(enabledProperties())
				.analyze(context(null), goalTarget());

		assertThat(aiClient.calls).isZero();
		assertThat(result.annotationActions()).isEmpty();
	}

	@Test
	void skipsWhenAiDisabled() {
		AiProperties disabled = new AiProperties(); // enabled defaults to false
		AssistantResult result = newAssistant(disabled)
				.analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		assertThat(aiClient.calls).isZero();
		assertThat(result.annotationActions()).isEmpty();
	}

	@Test
	void skipsWhenProjectNotAllowed() {
		AiProperties props = enabledProperties();
		props.setProjectAllowlist(List.of("999")); // run's project id is 2
		AssistantResult result = newAssistant(props)
				.analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		assertThat(aiClient.calls).isZero();
		assertThat(result.annotationActions()).isEmpty();
	}

	@Test
	void callsProviderWhenRequirementsReviewEnabledAndAllowed() {
		when(packBuilder.build(any())).thenReturn(mock(EntityContextPack.class));

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
	void skipsProviderWhenContextExceedsInputCap() {
		when(packBuilder.build(any())).thenReturn(mock(EntityContextPack.class));
		AiProperties props = enabledProperties();
		props.setMaxInputTokens(-1); // force any estimate to exceed the cap

		AssistantResult result = newAssistant(props)
				.analyze(context("REQUIREMENTS_REVIEW"), goalTarget());

		assertThat(aiClient.calls).isZero();
		assertThat(result.annotationActions()).isEmpty();
		assertThat(result.messages()).anyMatch(m -> m.text().contains("maxInputTokens"));
	}

	@Test
	void mapsFindingsToAnnotationActions() {
		when(packBuilder.build(any())).thenReturn(mock(EntityContextPack.class));
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

	private RequirementsReviewAssistant newAssistant(AiProperties properties) {
		return new RequirementsReviewAssistant(aiClient, packBuilder, properties, usageRepository,
				objectMapper);
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

		@Override
		public AiAnalysisResponse analyze(AiAnalysisRequest request) {
			this.calls++;
			this.lastRequest = request;
			return response;
		}
	}
}
