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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Draft;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Problem;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Reference;
import com.rreganjr.requel.assistant.ai.IdeaToRequirementsService.Idea;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantMessage;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.persistence.IdeaDraftEntity;
import com.rreganjr.requel.assistant.core.persistence.IdeaDraftRepository;
import com.rreganjr.requel.assistant.core.persistence.IdeaEntity;
import com.rreganjr.requel.assistant.core.persistence.IdeaRepository;
import com.rreganjr.requel.assistant.core.persistence.IdeaStatus;

/**
 * Saves a product manager's idea and generates its requirement drafts in the background, so a slow
 * or failing provider never holds a web request. {@link #submit} stores the idea as
 * {@code PENDING} and queues generation; {@link #run} takes the idea, calls
 * {@link IdeaToRequirementsService#draft} with no transaction open, then replaces the idea's drafts
 * and sets {@code DRAFTED} in a short transaction. A failure sets {@code FAILED} with the error
 * and a skipped call sets {@code SKIPPED}; both keep the idea, and {@link #retry} runs it again on
 * the saved text.
 *
 * <p>
 * The idea row is the lock: {@link IdeaRepository#claim} is one atomic update, so concurrent
 * retries start one attempt, and an attempt's result is stored only while it is still the
 * latest one. An idea stuck {@code GENERATING} (a crash mid-call) can be retried once it has not
 * changed for {@link #STALE_AFTER}.
 *
 * <p>
 * The bean exists only when {@code requel.ai.enabled=true}, like {@link IdeaToRequirementsService}.
 */
@Component
@ConditionalOnProperty(name = "requel.ai.enabled", havingValue = "true")
public class IdeaGenerationService {

	private static final Logger log = LoggerFactory.getLogger(IdeaGenerationService.class);

	/** A generating idea untouched for this long is treated as abandoned and may be retried. */
	static final Duration STALE_AFTER = Duration.ofMinutes(10);

	private static final int ERROR_SUMMARY_LENGTH = 1000;
	private static final int RESULT_SUMMARY_LENGTH = 2000;

	/** The names stored with the idea, so a retry sends what the first attempt did. */
	public record ExistingNames(List<String> goals, List<String> actors,
			List<String> glossaryTerms) {
	}

	/**
	 * An idea and, once generated, its drafts.
	 *
	 * @param status an {@link IdeaStatus} name
	 * @param errorSummary why the last attempt failed or was skipped, or null
	 * @param summary the model's one-sentence summary, or null before the first success
	 * @param messages the model's open questions
	 * @param problems what validation dropped or adjusted
	 * @param retryable whether {@link #retry} would be accepted now
	 */
	public record IdeaView(String id, Long projectId, String ideaText, String status,
			String errorSummary, String summary, int attempts, boolean retryable,
			List<String> messages, List<Problem> problems, List<Draft> drafts, Instant createdAt,
			Instant updatedAt) {
	}

	private final IdeaToRequirementsService ideaService;
	private final IdeaRepository ideas;
	private final IdeaDraftRepository drafts;
	private final TaskExecutor taskExecutor;
	private final ObjectMapper objectMapper;
	private final Clock clock;
	/** Joins the caller's transaction: the idea row commits with the caller's work. */
	private final TransactionOperations required;
	/** Always its own transaction: the writes made from the executor thread. */
	private final TransactionOperations requiresNew;

	@Autowired
	public IdeaGenerationService(IdeaToRequirementsService ideaService, IdeaRepository ideas,
			IdeaDraftRepository drafts, @Qualifier("assistantTaskExecutor") TaskExecutor taskExecutor,
			PlatformTransactionManager transactionManager, ObjectMapper objectMapper) {
		this(ideaService, ideas, drafts, taskExecutor,
				template(transactionManager, TransactionDefinition.PROPAGATION_REQUIRED),
				template(transactionManager, TransactionDefinition.PROPAGATION_REQUIRES_NEW),
				objectMapper, Clock.systemUTC());
	}

	IdeaGenerationService(IdeaToRequirementsService ideaService, IdeaRepository ideas,
			IdeaDraftRepository drafts, TaskExecutor taskExecutor, TransactionOperations required,
			TransactionOperations requiresNew, ObjectMapper objectMapper, Clock clock) {
		this.ideaService = Objects.requireNonNull(ideaService, "ideaService");
		this.ideas = Objects.requireNonNull(ideas, "ideas");
		this.drafts = Objects.requireNonNull(drafts, "drafts");
		this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor");
		this.required = Objects.requireNonNull(required, "required");
		this.requiresNew = Objects.requireNonNull(requiresNew, "requiresNew");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	private static TransactionOperations template(PlatformTransactionManager manager,
			int propagation) {
		TransactionTemplate template = new TransactionTemplate(
				Objects.requireNonNull(manager, "transactionManager"));
		template.setPropagationBehavior(propagation);
		return template;
	}

	/**
	 * Save {@code idea} for {@code projectId} and queue its generation. Returns at once; poll
	 * {@link #get} for the outcome.
	 *
	 * @param user who submitted it
	 * @param idea the text, and the names the project already has (so they are not proposed again)
	 * @return the saved idea's id
	 * @throws IllegalArgumentException if the project or the text is missing
	 */
	public String submit(Long projectId, UserRef user, Idea idea) {
		if (projectId == null) {
			throw new IllegalArgumentException("An idea belongs to a project");
		}
		if (user == null) {
			throw new IllegalArgumentException("An idea needs the user who submitted it");
		}
		if (idea == null || idea.ideaText() == null || idea.ideaText().isBlank()) {
			throw new IllegalArgumentException("An idea is required");
		}
		String names = write(new ExistingNames(orEmpty(idea.existingGoals()),
				orEmpty(idea.existingActors()), orEmpty(idea.existingGlossaryTerms())));
		IdeaEntity entity = new IdeaEntity(UUID.randomUUID(), projectId, user.userId(),
				user.username(), idea.ideaText().strip(), names, clock.instant());
		required.executeWithoutResult(status -> ideas.save(entity));
		schedule(entity.getId());
		return entity.getId();
	}

	/**
	 * Run generation for an idea that failed or was skipped, again, on its saved text. The old
	 * drafts are replaced when it succeeds.
	 *
	 * @throws IllegalArgumentException if there is no such idea
	 * @throws IllegalStateException if the idea is not retryable (already drafted, or running)
	 */
	public void retry(String ideaId) {
		IdeaEntity idea = ideas.findById(ideaId)
				.orElseThrow(() -> new IllegalArgumentException("No such idea: " + ideaId));
		if (!retryable(idea)) {
			throw new IllegalStateException("The idea is " + idea.getStatus()
					+ "; only a failed or skipped idea can be retried");
		}
		schedule(ideaId);
	}

	/** The idea and its drafts, or empty when there is no such idea. */
	public Optional<IdeaView> get(String ideaId) {
		return ideas.findById(ideaId).map(this::view);
	}

	/** The project's ideas, newest first, without their drafts. */
	public List<IdeaView> list(Long projectId) {
		List<IdeaView> views = new ArrayList<>();
		for (IdeaEntity idea : ideas.findByProjectIdOrderByCreatedAtDesc(projectId)) {
			views.add(view(idea, List.of()));
		}
		return views;
	}

	/**
	 * One generation attempt, on the calling thread: take the idea, call the provider with no
	 * transaction open, store the outcome. Does nothing when the idea can't be taken (it is
	 * already running or already drafted). Never throws for a generation failure; it is recorded
	 * on the idea.
	 */
	public void run(String ideaId) {
		Instant now = clock.instant();
		UUID runId = UUID.randomUUID();
		Integer claimed = requiresNew.execute(
				status -> ideas.claim(ideaId, runId.toString(), now, now.minus(STALE_AFTER)));
		if (claimed == null || claimed == 0) {
			log.info("Idea {} was not taken for generation (running, drafted or gone)", ideaId);
			return;
		}
		IdeaEntity idea = requiresNew.execute(status -> ideas.findById(ideaId).orElse(null));
		if (idea == null) {
			return;
		}
		try {
			ExistingNames names = readNames(idea);
			Idea input = new Idea(idea.getIdeaText(), names.goals(), names.actors(),
					names.glossaryTerms());
			UserRef user = new UserRef(
					idea.getCreatedByUserId() == null ? 0L : idea.getCreatedByUserId(),
					idea.getCreatedByUsername() == null ? "unknown" : idea.getCreatedByUsername());
			// the idea service reads neither user; the assistant user is the submitter's
			AssistantContext context = new AssistantContext(runId, user, user,
					EntityRef.of("Project", idea.getProjectId()), IdeaToRequirements.TASK_TYPE,
					Locale.ROOT, clock, Map.of());
			IdeaRequirementsDrafts result = ideaService.draft(context, input);
			store(ideaId, runId, result);
		} catch (AssistantException | RuntimeException e) {
			log.warn("Idea {} generation failed (run {}): {}", ideaId, runId, e.getMessage(), e);
			fail(ideaId, runId, e.getMessage() == null ? e.toString() : e.getMessage());
		}
	}

	// ---- scheduling ------------------------------------------------------

	/** Queue {@link #run}; inside a caller's transaction, only once it has committed. */
	private void schedule(String ideaId) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					submitTask(ideaId);
				}
			});
		} else {
			submitTask(ideaId);
		}
	}

	private void submitTask(String ideaId) {
		try {
			taskExecutor.execute(() -> run(ideaId));
		} catch (TaskRejectedException e) {
			// otherwise the idea would sit PENDING with nothing to run it
			log.warn("Idea {} generation was rejected by the executor: {}", ideaId, e.getMessage());
			fail(ideaId, null, "The server is busy; retry in a moment.");
		}
	}

	// ---- storing the outcome ---------------------------------------------

	private void store(String ideaId, UUID runId, IdeaRequirementsDrafts result) {
		requiresNew.executeWithoutResult(status -> {
			IdeaEntity idea = ideas.findById(ideaId).orElse(null);
			if (!ownsAttempt(idea, runId)) {
				return;
			}
			Instant now = clock.instant();
			drafts.deleteByIdeaId(ideaId);
			if (result.skipped()) {
				idea.setStatus(IdeaStatus.SKIPPED.name());
				idea.setErrorSummary(truncate(result.skippedReason(), ERROR_SUMMARY_LENGTH));
			} else {
				List<IdeaDraftEntity> rows = new ArrayList<>();
				int order = 0;
				for (Draft draft : result.drafts()) {
					rows.add(new IdeaDraftEntity(UUID.randomUUID(), ideaId, order++,
							draft.kind().name(), draft.name(), draft.text(), draft.note(),
							draft.assumption(), write(draft.evidence()), draft.evidenceUnverified(),
							draft.confidence(), write(draft.references()), now));
				}
				drafts.saveAll(rows);
				idea.setStatus(IdeaStatus.DRAFTED.name());
				idea.setErrorSummary(null);
				idea.setResultSummary(truncate(result.summary(), RESULT_SUMMARY_LENGTH));
				idea.setMessagesJson(write(messageTexts(result.messages())));
				idea.setProblemsJson(write(result.problems()));
			}
			idea.setUpdatedAt(now);
			ideas.save(idea);
		});
	}

	/** @param runId the attempt that failed, or null when no attempt started (a rejected task) */
	private void fail(String ideaId, UUID runId, String message) {
		try {
			requiresNew.executeWithoutResult(status -> {
				IdeaEntity idea = ideas.findById(ideaId).orElse(null);
				if (idea == null || (runId != null && !ownsAttempt(idea, runId))
						|| (runId == null && idea.getIdeaStatus() != IdeaStatus.PENDING)) {
					return;
				}
				idea.setStatus(IdeaStatus.FAILED.name());
				idea.setErrorSummary(truncate(message, ERROR_SUMMARY_LENGTH));
				idea.setUpdatedAt(clock.instant());
				ideas.save(idea);
			});
		} catch (RuntimeException e) {
			log.error("Could not record the failure of idea {}: {}", ideaId, e.getMessage(), e);
		}
	}

	/** True while {@code runId} is the idea's latest attempt and it is still generating. */
	private static boolean ownsAttempt(IdeaEntity idea, UUID runId) {
		if (idea == null || idea.getIdeaStatus() != IdeaStatus.GENERATING
				|| !runId.toString().equals(idea.getLastRunId())) {
			log.info("Discarding the result of run {}: the idea moved on", runId);
			return false;
		}
		return true;
	}

	// ---- reading ---------------------------------------------------------

	private boolean retryable(IdeaEntity idea) {
		IdeaStatus status = idea.getIdeaStatus();
		return status == IdeaStatus.FAILED || status == IdeaStatus.SKIPPED
				|| (status == IdeaStatus.GENERATING
						&& idea.getUpdatedAt().isBefore(clock.instant().minus(STALE_AFTER)));
	}

	private IdeaView view(IdeaEntity idea) {
		List<Draft> rows = new ArrayList<>();
		for (IdeaDraftEntity row : drafts.findByIdeaIdOrderBySortOrder(idea.getId())) {
			rows.add(new Draft(kind(row.getKind()), row.getName(), row.getDraftText(),
					row.getNote(), row.isAssumption(),
					read(row.getEvidenceJson(), new TypeReference<List<String>>() {
					}), row.isEvidenceUnverified(), row.getConfidence(),
					read(row.getReferencesJson(), new TypeReference<List<Reference>>() {
					})));
		}
		return view(idea, rows);
	}

	private IdeaView view(IdeaEntity idea, List<Draft> rows) {
		return new IdeaView(idea.getId(), idea.getProjectId(), idea.getIdeaText(),
				idea.getStatus(), idea.getErrorSummary(), idea.getResultSummary(),
				idea.getAttempts(), retryable(idea),
				read(idea.getMessagesJson(), new TypeReference<List<String>>() {
				}), read(idea.getProblemsJson(), new TypeReference<List<Problem>>() {
				}), rows, idea.getCreatedAt(), idea.getUpdatedAt());
	}

	private static IdeaRequirementsDrafts.Kind kind(String name) {
		return IdeaRequirementsDrafts.Kind.valueOf(name);
	}

	private ExistingNames readNames(IdeaEntity idea) {
		if (idea.getExistingNamesJson() == null) {
			return new ExistingNames(List.of(), List.of(), List.of());
		}
		try {
			return objectMapper.readValue(idea.getExistingNamesJson(), ExistingNames.class);
		} catch (JsonProcessingException e) {
			log.warn("Idea {}: could not read the stored names, sending none: {}", idea.getId(),
					e.getOriginalMessage());
			return new ExistingNames(List.of(), List.of(), List.of());
		}
	}

	// ---- JSON ------------------------------------------------------------

	private String write(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not store idea data: " + e.getOriginalMessage(),
					e);
		}
	}

	private <T> List<T> read(String json, TypeReference<List<T>> type) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, type);
		} catch (JsonProcessingException e) {
			log.warn("Could not read stored idea data: {}", e.getOriginalMessage());
			return List.of();
		}
	}

	private static List<String> messageTexts(List<AssistantMessage> messages) {
		List<String> texts = new ArrayList<>();
		for (AssistantMessage message : messages) {
			if (message.text() != null && !message.text().isBlank()) {
				texts.add(message.text());
			}
		}
		return texts;
	}

	private static List<String> orEmpty(List<String> values) {
		return values == null ? List.of() : values;
	}

	private static String truncate(String value, int max) {
		return value == null || value.length() <= max ? value : value.substring(0, max - 1) + "…";
	}
}
