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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.transaction.support.TransactionOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.rreganjr.requel.assistant.ai.IdeaGenerationService.IdeaView;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Draft;
import com.rreganjr.requel.assistant.ai.IdeaToRequirementsService.Idea;
import com.rreganjr.requel.assistant.api.AssistantMessage;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.context.EntityContextPackBuilder;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageRepository;
import com.rreganjr.requel.assistant.core.persistence.IdeaDraftEntity;
import com.rreganjr.requel.assistant.core.persistence.IdeaDraftRepository;
import com.rreganjr.requel.assistant.core.persistence.IdeaEntity;
import com.rreganjr.requel.assistant.core.persistence.IdeaRepository;

class IdeaGenerationServiceTest {

	private static final String IDEA_TEXT = "Let gym members book a class from their phone.";
	private static final UserRef USER = new UserRef(3L, "pm");

	private final EntityContextPackBuilder packBuilder = mock(EntityContextPackBuilder.class);
	private final AssistantUsageRepository usageRepository = mock(AssistantUsageRepository.class);
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final RecordingAiClient aiClient = new RecordingAiClient();

	// in-memory stand-ins for the repositories
	private final Map<String, IdeaEntity> ideaRows = new HashMap<>();
	private final List<IdeaDraftEntity> draftRows = new ArrayList<>();
	private final IdeaRepository ideas = mock(IdeaRepository.class);
	private final IdeaDraftRepository drafts = mock(IdeaDraftRepository.class);

	/** Runs the task on the calling thread. */
	private TaskExecutor executor = Runnable::run;

	IdeaGenerationServiceTest() {
		when(ideas.save(any(IdeaEntity.class))).thenAnswer(call -> {
			IdeaEntity entity = call.getArgument(0);
			ideaRows.put(entity.getId(), entity);
			return entity;
		});
		when(ideas.findById(anyString())).thenAnswer(
				call -> Optional.ofNullable(ideaRows.get(call.<String>getArgument(0))));
		when(ideas.claim(anyString(), anyString(), any(Instant.class), any(Instant.class)))
				.thenAnswer(call -> claim(call.getArgument(0), call.getArgument(1),
						call.getArgument(2), call.getArgument(3)));
		when(drafts.saveAll(any())).thenAnswer(call -> {
			Iterable<IdeaDraftEntity> rows = call.getArgument(0);
			rows.forEach(draftRows::add);
			return List.copyOf(draftRows);
		});
		when(drafts.deleteByIdeaId(anyString())).thenAnswer(call -> {
			String id = call.getArgument(0);
			int before = draftRows.size();
			draftRows.removeIf(row -> row.getIdeaId().equals(id));
			return before - draftRows.size();
		});
		when(drafts.findByIdeaIdOrderBySortOrder(anyString())).thenAnswer(call -> {
			String id = call.getArgument(0);
			return draftRows.stream().filter(row -> row.getIdeaId().equals(id))
					.sorted(Comparator.comparingInt(IdeaDraftEntity::getSortOrder)).toList();
		});
	}

	/** The same rule as {@link IdeaRepository#claim}. */
	private int claim(String id, String runId, Instant now, Instant staleBefore) {
		IdeaEntity idea = ideaRows.get(id);
		if (idea == null) {
			return 0;
		}
		String status = idea.getStatus();
		boolean takeable = status.equals("PENDING") || status.equals("FAILED")
				|| status.equals("SKIPPED")
				|| (status.equals("GENERATING") && idea.getUpdatedAt().isBefore(staleBefore));
		if (!takeable) {
			return 0;
		}
		idea.setStatus("GENERATING");
		idea.setAttempts(idea.getAttempts() + 1);
		idea.setLastRunId(runId);
		idea.setErrorSummary(null);
		idea.setUpdatedAt(now);
		return 1;
	}

	@Test
	void submitSavesTheIdeaAndStoresTheDrafts() throws Exception {
		aiClient.response = reply(
				finding("ACTOR", "Gym member", "A person who attends classes."),
				finding("GOAL", "Book remotely", "Members reserve a place without calling."),
				finding("USE_CASE", "Receive reminder", "A member is reminded before class.",
						"GOAL:Book remotely", "ACTOR:Gym member"));
		IdeaGenerationService service = service(enabledProperties());

		String id = service.submit(2L, USER,
				new Idea("  " + IDEA_TEXT + " ", List.of("Existing goal"), null, List.of()));

		IdeaView view = service.get(id).orElseThrow();
		assertThat(view.status()).isEqualTo("DRAFTED");
		assertThat(view.ideaText()).isEqualTo(IDEA_TEXT);
		assertThat(view.projectId()).isEqualTo(2L);
		assertThat(view.attempts()).isEqualTo(1);
		assertThat(view.summary()).isEqualTo("summary");
		assertThat(view.messages()).containsExactly("What if a class is full?");
		assertThat(view.retryable()).isFalse();
		assertThat(view.drafts()).extracting(Draft::name)
				.containsExactly("Gym member", "Book remotely", "Receive reminder");
		Draft useCase = view.drafts().get(2);
		assertThat(useCase.references()).extracting(IdeaRequirementsDrafts.Reference::name)
				.containsExactly("Gym member", "Book remotely");

		// the names saved with the idea reached the provider
		Idea sent = (Idea) aiClient.lastRequest.contextPacks().get(0);
		assertThat(sent.existingGoals()).containsExactly("Existing goal");
	}

	@Test
	void aFailedGenerationKeepsTheIdeaAndRetryReplacesTheDrafts() throws Exception {
		aiClient.failure = new AiAnalysisException("provider exploded");
		IdeaGenerationService service = service(enabledProperties());

		String id = service.submit(2L, USER, idea());

		IdeaView failed = service.get(id).orElseThrow();
		assertThat(failed.status()).isEqualTo("FAILED");
		assertThat(failed.errorSummary()).contains("provider exploded");
		assertThat(failed.retryable()).isTrue();
		assertThat(failed.drafts()).isEmpty();
		assertThat(failed.ideaText()).isEqualTo(IDEA_TEXT);

		aiClient.failure = null;
		aiClient.response = reply(finding("ACTOR", "Gym member", "A person who attends classes."));
		service.retry(id);

		IdeaView retried = service.get(id).orElseThrow();
		assertThat(retried.status()).isEqualTo("DRAFTED");
		assertThat(retried.errorSummary()).isNull();
		assertThat(retried.attempts()).isEqualTo(2);
		assertThat(retried.drafts()).hasSize(1);

		// a second successful run replaces, not adds to, the stored drafts
		ideaRows.get(id).setStatus("FAILED");
		service.retry(id);
		assertThat(service.get(id).orElseThrow().drafts()).hasSize(1);
	}

	@Test
	void aSkippedCallIsRecordedAndRetryable() throws Exception {
		IdeaGenerationService service = service(new AiProperties()); // AI off: the call is skipped

		String id = service.submit(2L, USER, idea());

		IdeaView view = service.get(id).orElseThrow();
		assertThat(view.status()).isEqualTo("SKIPPED");
		assertThat(view.errorSummary()).contains("disabled");
		assertThat(view.retryable()).isTrue();
		assertThat(aiClient.calls).isZero();
	}

	@Test
	void anIdeaThatIsDraftedCannotBeRetried() throws Exception {
		IdeaGenerationService service = service(enabledProperties());
		String id = service.submit(2L, USER, idea());
		int calls = aiClient.calls;

		assertThatThrownBy(() -> service.retry(id)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> service.retry("nope"))
				.isInstanceOf(IllegalArgumentException.class);
		service.run(id); // already drafted: not taken, no second call
		assertThat(aiClient.calls).isEqualTo(calls);
	}

	@Test
	void aGeneratingIdeaIsRetryableOnlyOnceItIsStale() throws Exception {
		IdeaGenerationService service = service(enabledProperties());
		String id = service.submit(2L, USER, idea());
		IdeaEntity entity = ideaRows.get(id);
		entity.setStatus("GENERATING");
		entity.setUpdatedAt(Instant.now());

		assertThat(service.get(id).orElseThrow().retryable()).isFalse();
		assertThatThrownBy(() -> service.retry(id)).isInstanceOf(IllegalStateException.class);

		entity.setUpdatedAt(Instant.now().minus(IdeaGenerationService.STALE_AFTER).minusSeconds(1));
		assertThat(service.get(id).orElseThrow().retryable()).isTrue();
		service.retry(id);
		assertThat(service.get(id).orElseThrow().status()).isEqualTo("DRAFTED");
	}

	@Test
	void aRejectedTaskFailsTheIdeaInsteadOfLeavingItPending() {
		executor = task -> {
			throw new TaskRejectedException("queue full");
		};
		IdeaGenerationService service = service(enabledProperties());

		String id = service.submit(2L, USER, idea());

		IdeaView view = service.get(id).orElseThrow();
		assertThat(view.status()).isEqualTo("FAILED");
		assertThat(view.errorSummary()).contains("busy");
		assertThat(aiClient.calls).isZero();
	}

	@Test
	void anUnexpectedErrorFailsTheIdeaRatherThanLeavingItGenerating() {
		aiClient.runtimeFailure = new IllegalStateException("bug");
		IdeaGenerationService service = service(enabledProperties());

		String id = service.submit(2L, USER, idea());

		assertThat(service.get(id).orElseThrow().status()).isEqualTo("FAILED");
	}

	@Test
	void aResultFromASupersededAttemptIsDiscarded() throws Exception {
		IdeaGenerationService service = service(enabledProperties());
		String id = service.submit(2L, USER, idea());
		IdeaEntity entity = ideaRows.get(id);
		// another attempt took over while this one's call was out
		aiClient.beforeReturn = () -> entity.setLastRunId("someone-else");
		entity.setStatus("FAILED");

		service.run(id);

		assertThat(entity.getStatus()).isEqualTo("GENERATING");
		assertThat(draftRows).isEmpty();
	}

	@Test
	void rejectsAMissingProjectUserOrText() {
		IdeaGenerationService service = service(enabledProperties());

		assertThatThrownBy(() -> service.submit(null, USER, idea()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.submit(2L, null, idea()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.submit(2L, USER, new Idea("  ", null, null, null)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.submit(2L, USER, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(ideaRows).isEmpty();
	}

	private IdeaGenerationService service(AiProperties properties) {
		IdeaToRequirementsService ideaService = new IdeaToRequirementsService(
				new AiDefinitionExecutorFactory(aiClient, packBuilder, properties,
						usageRepository, objectMapper));
		TransactionOperations none = TransactionOperations.withoutTransaction();
		return new IdeaGenerationService(ideaService, ideas, drafts, task -> executor.execute(task),
				none, none, objectMapper, Clock.systemUTC());
	}

	private static Idea idea() {
		return new Idea(IDEA_TEXT, List.of(), List.of(), List.of());
	}

	private static AiProperties enabledProperties() {
		AiProperties properties = new AiProperties();
		properties.setEnabled(true);
		return properties;
	}

	private static AiAnalysisResponse reply(AiFindingDraft... findings) {
		return new AiAnalysisResponse("summary", NullNode.getInstance(), List.of(findings),
				List.of(AssistantMessage.warning("What if a class is full?")),
				AiUsage.noop("noop", Duration.ZERO), Map.of());
	}

	private static AiFindingDraft finding(String type, String name, String text,
			String... participants) {
		return new AiFindingDraft(type, null, 0.8, List.of(), text, null, List.of(),
				Map.of(ReviewResultMapper.SUGGESTED_ENTITY_NAME, name,
						ReviewResultMapper.PARTICIPANTS, List.of(participants)));
	}

	/** Records invocations and returns a configurable response (no real provider). */
	private static final class RecordingAiClient implements AiAnalysisClient {
		private int calls;
		private AiAnalysisRequest lastRequest;
		private AiAnalysisResponse response = reply();
		private AiAnalysisException failure;
		private RuntimeException runtimeFailure;
		private Runnable beforeReturn;

		@Override
		public AiAnalysisResponse analyze(AiAnalysisRequest request) throws AiAnalysisException {
			this.calls++;
			this.lastRequest = request;
			if (runtimeFailure != null) {
				throw runtimeFailure;
			}
			if (failure != null) {
				throw failure;
			}
			if (beforeReturn != null) {
				beforeReturn.run();
			}
			return response;
		}
	}
}
