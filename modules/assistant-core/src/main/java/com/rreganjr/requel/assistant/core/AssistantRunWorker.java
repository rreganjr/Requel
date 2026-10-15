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
package com.rreganjr.requel.assistant.core;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantRegistry;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.DefinitionBacked;
import com.rreganjr.requel.project.TargetFingerprint;

/**
 * Runs queued assistant work in two short transactions (issue #247).
 * <p>
 * A run used to be one {@code REQUIRES_NEW} transaction spanning the whole NLP /
 * AI analysis. Because annotations use {@code IDENTITY} ids, the first
 * assistant's findings were inserted immediately and their row locks (including
 * the InnoDB shared locks an FK insert takes on the parent {@code goals} /
 * {@code stories} / {@code pods} rows) were then held for as long as the
 * remaining assistants took to analyze - several seconds per entity. Any user
 * command touching those rows in the meantime, a delete above all, blocked or
 * deadlocked; and a delete that read the entity before the run committed then
 * failed its FK check once it had.
 * <p>
 * Now the <em>analyze</em> phase runs in its own transaction that commits (or
 * rolls back) before a single finding is written, and the <em>apply</em> phase
 * is a second, short transaction that first re-checks the target still exists:
 * if the user deleted it while the analysis ran, the run is skipped instead of
 * inserting rows for a row that is gone (the "LATEST FOREIGN KEY ERROR" MySQL
 * reported during the e2e suite).
 * <p>
 * That target re-check is necessary but not sufficient (issue #279). It looks at the
 * target entity, never the project the findings are <em>grouped under</em>, and even when
 * it passes nothing stops the delete from committing immediately afterwards - the check
 * and the inserts were not serialized against it at all. Because
 * {@code annotations.grouping_object_id} is an {@code @Any} soft reference with no foreign
 * key, the losing run's rows are accepted by the database and become unreachable orphans
 * rather than a loud failure. So the apply phase now also takes the project row's write
 * lock through {@link AssistantProjectGate} before writing anything, and a run that finds
 * the project gone records {@link AssistantRunStatus#CANCELLED} instead of inserting.
 * <p>
 * The analyze transaction still held a connection for as long as an AI assistant waited on its
 * model (issue #363), so a bulk review could hold most of the pool while web requests stalled.
 * A {@link StagedAssistant} now only <em>prepares</em> its call inside the analyze transaction;
 * the call itself is made after that transaction commits and before apply begins, with no
 * transaction open.
 */
@Component
public class AssistantRunWorker {

	private static final Logger log = LoggerFactory.getLogger(AssistantRunWorker.class);

	/**
	 * Issue #260: result metadata key - how many of the result's findings cited evidence that is
	 * not in the entity's text.
	 */
	public static final String EVIDENCE_UNVERIFIED = "evidenceUnverified";
	/** #263: result metadata, the findings whose type is not in the definition's vocabulary. */
	public static final String VOCABULARY_MISSES = "vocabularyMisses";
	/** #263: result metadata, other assistant ids whose findings on the target this run retires. */
	public static final String RETIRES_ASSISTANTS = "retiresAssistants";

	private final AssistantRunStore runStore;
	private final AssistantRegistry assistantRegistry;
	private final AssistantResultApplicator resultApplicator;
	private final List<AssistantTargetLoader> targetLoaders;
	private final AssistantProjectGate projectGate;
	private final Clock clock;
	private final TransactionOperations analyzeTransaction;
	private final TransactionOperations applyTransaction;

	@Autowired
	public AssistantRunWorker(AssistantRunStore runStore, AssistantRegistry assistantRegistry,
			AssistantResultApplicator resultApplicator, List<AssistantTargetLoader> targetLoaders,
			AssistantProjectGate projectGate, PlatformTransactionManager transactionManager) {
		this(runStore, assistantRegistry, resultApplicator, targetLoaders, projectGate,
				Clock.systemUTC(), requiresNew(transactionManager), requiresNew(transactionManager));
	}

	/**
	 * Test constructor: both phases run inline, without a transaction manager, and the
	 * project gate is always open (the tests that care about the gate pass their own).
	 */
	AssistantRunWorker(AssistantRunStore runStore, AssistantRegistry assistantRegistry,
			AssistantResultApplicator resultApplicator, List<AssistantTargetLoader> targetLoaders) {
		this(runStore, assistantRegistry, resultApplicator, targetLoaders, Clock.systemUTC());
	}

	/**
	 * Test constructor: both phases run inline, without a transaction manager, and the
	 * project gate is always open.
	 */
	AssistantRunWorker(AssistantRunStore runStore, AssistantRegistry assistantRegistry,
			AssistantResultApplicator resultApplicator, List<AssistantTargetLoader> targetLoaders,
			Clock clock) {
		this(runStore, assistantRegistry, resultApplicator, targetLoaders,
				projectRef -> AssistantProjectGate.State.OPEN, clock,
				TransactionOperations.withoutTransaction(), TransactionOperations.withoutTransaction());
	}

	/**
	 * Test constructor: both phases run inline, with the supplied project gate.
	 */
	AssistantRunWorker(AssistantRunStore runStore, AssistantRegistry assistantRegistry,
			AssistantResultApplicator resultApplicator, List<AssistantTargetLoader> targetLoaders,
			AssistantProjectGate projectGate) {
		this(runStore, assistantRegistry, resultApplicator, targetLoaders, projectGate,
				Clock.systemUTC(), TransactionOperations.withoutTransaction(),
				TransactionOperations.withoutTransaction());
	}

	/**
	 * Test constructor: supplied transaction templates, project gate always open.
	 */
	AssistantRunWorker(AssistantRunStore runStore, AssistantRegistry assistantRegistry,
			AssistantResultApplicator resultApplicator, List<AssistantTargetLoader> targetLoaders,
			Clock clock, TransactionOperations analyzeTransaction,
			TransactionOperations applyTransaction) {
		this(runStore, assistantRegistry, resultApplicator, targetLoaders,
				projectRef -> AssistantProjectGate.State.OPEN, clock, analyzeTransaction,
				applyTransaction);
	}

	AssistantRunWorker(AssistantRunStore runStore, AssistantRegistry assistantRegistry,
			AssistantResultApplicator resultApplicator, List<AssistantTargetLoader> targetLoaders,
			AssistantProjectGate projectGate, Clock clock, TransactionOperations analyzeTransaction,
			TransactionOperations applyTransaction) {
		this.runStore = runStore;
		this.assistantRegistry = assistantRegistry;
		this.resultApplicator = resultApplicator;
		this.targetLoaders = List.copyOf(targetLoaders);
		this.projectGate = Objects.requireNonNull(projectGate, "projectGate");
		this.clock = clock;
		this.analyzeTransaction = Objects.requireNonNull(analyzeTransaction, "analyzeTransaction");
		this.applyTransaction = Objects.requireNonNull(applyTransaction, "applyTransaction");
	}

	private static TransactionOperations requiresNew(PlatformTransactionManager transactionManager) {
		TransactionTemplate template = new TransactionTemplate(
				Objects.requireNonNull(transactionManager, "transactionManager"));
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template;
	}

	/**
	 * Execute a queued run: analyze in one transaction, make any staged model calls with
	 * none open (#363), and apply the findings in a second short one. Never joins a caller's
	 * transaction.
	 *
	 * @param runId
	 *            the queued run.
	 */
	public void run(UUID runId) {
		Objects.requireNonNull(runId, "runId");
		Optional<AssistantRunRecord> recordValue = runStore.findRun(runId);
		if (recordValue.isEmpty()) {
			// #259: this used to be silent, leaving a QUEUED row that never runs when the row was
			// not yet visible (queued inside an uncommitted transaction).
			log.warn("Assistant run {} not found when the worker started; not run", runId);
			return;
		}
		AssistantRunRecord record = recordValue.get();

		runStore.markRunning(runId);
		try {
			// Phase 1 - analyze. Reads the target and runs every matching assistant; no
			// finding is written, so no annotation / join-table locks are taken while the
			// (slow) NLP or AI work happens.
			Analysis analysis = analyzeTransaction.execute(status -> analyze(record));
			if (analysis.skipReason != null) {
				runStore.markSkipped(runId, analysis.skipReason);
				return;
			}
			// #363: the model calls, between the phases, with no transaction or connection held.
			completeStages(record, analysis);
			analysis.collect();
			// #259: every matching assistant threw, so there is nothing to apply and the run did
			// not do its job. (Some failing is PARTIAL, below.) Each failure was already logged.
			// #355: keep the assistants' own summary of the run (best effort).
			recordResultSummary(runId, analysis.results);
			// #260: which definitions ran, and how many findings cited missing evidence.
			recordDefinitions(runId, analysis.definitions);
			recordEvidenceUnverified(runId, analysis.results);
			recordVocabularyMisses(runId, analysis.results);
			if (analysis.allFailed()) {
				runStore.markFailed(runId,
						new AssistantWorkerException(String.join("; ", analysis.problems)));
				return;
			}

			// Phase 2 - apply, briefly. Gate on the project row and re-check the target: the
			// user may have deleted either while phase 1 ran, in which case writing findings
			// would produce FK violations against rows that are gone - or, worse, silent
			// orphans under the soft-FK columns that have no constraint at all (#279).
			Outcome outcome = applyTransaction.execute(status -> apply(record, analysis));
			// TransactionOperations.execute is declared @Nullable; apply() never returns
			// null, so a null here can only mean "nothing said otherwise" - treat as applied.
			if (outcome == null) {
				runStore.markSucceeded(runId);
				return;
			}
			switch (outcome.status) {
				case SKIPPED -> runStore.markSkipped(runId, outcome.reason);
				case CANCELLED -> runStore.markCancelled(runId, outcome.reason);
				default -> {
					List<String> problems = new ArrayList<>(analysis.problems);
					problems.addAll(outcome.problems);
					if (problems.isEmpty()) {
						runStore.markSucceeded(runId);
					} else {
						runStore.markPartial(runId, String.join("; ", problems));
					}
				}
			}
		} catch (RuntimeException e) {
			runStore.markFailed(runId, e);
			throw new AssistantWorkerException("Assistant run failed: " + runId, e);
		}
	}

	/**
	 * Issue #355: join the non-blank summaries of the run's results and store them on the run. A
	 * failure to record never fails the run.
	 */
	private void recordResultSummary(UUID runId, List<AssistantResult> results) {
		List<String> summaries = new ArrayList<>();
		for (AssistantResult result : results) {
			if (result != null && result.summary() != null && !result.summary().isBlank()) {
				summaries.add(result.summary().strip());
			}
		}
		if (summaries.isEmpty()) {
			return;
		}
		try {
			runStore.recordResultSummary(runId, String.join("\n\n", summaries));
		} catch (RuntimeException e) {
			log.warn("Failed to record the result summary for run {}: {}", runId, e.getMessage(), e);
		}
	}

	/** Issue #260: record the definitions the run used. A failure never fails the run. */
	private void recordDefinitions(UUID runId, List<AssistantDefinition> definitions) {
		if (definitions == null || definitions.isEmpty()) {
			return;
		}
		List<String> keys = new ArrayList<>();
		List<String> versions = new ArrayList<>();
		List<String> sources = new ArrayList<>();
		for (AssistantDefinition definition : definitions) {
			keys.add(definition.key());
			versions.add(String.valueOf(definition.version()));
			sources.add(definition.source().name());
		}
		try {
			runStore.recordDefinitions(runId, String.join(",", keys), String.join(",", versions),
					String.join(",", sources));
		} catch (RuntimeException e) {
			log.warn("Failed to record the definitions for run {}: {}", runId, e.getMessage(), e);
		}
	}

	/** Issue #263: total the results' {@code vocabularyMisses} and record them, best effort. */
	private void recordVocabularyMisses(UUID runId, List<AssistantResult> results) {
		int total = 0;
		boolean any = false;
		for (AssistantResult result : results) {
			Object count = result == null ? null : result.metadata().get(VOCABULARY_MISSES);
			if (count instanceof Number number) {
				total += number.intValue();
				any = true;
			}
		}
		if (!any) {
			return;
		}
		try {
			runStore.recordVocabularyMisses(runId, total);
		} catch (RuntimeException e) {
			log.warn("Failed to record vocabulary misses for run {}: {}", runId, e.getMessage(), e);
		}
	}

	/**
	 * Issue #260: total the results' {@code evidenceUnverified} counts and record them. A failure
	 * never fails the run.
	 */
	private void recordEvidenceUnverified(UUID runId, List<AssistantResult> results) {
		int total = 0;
		boolean any = false;
		for (AssistantResult result : results) {
			Object count = result == null ? null : result.metadata().get(EVIDENCE_UNVERIFIED);
			if (count instanceof Number number) {
				total += number.intValue();
				any = true;
			}
		}
		if (!any) {
			return;
		}
		try {
			runStore.recordEvidenceUnverified(runId, total);
		} catch (RuntimeException e) {
			log.warn("Failed to record unverified evidence for run {}: {}", runId, e.getMessage(),
					e);
		}
	}

	/**
	 * @deprecated since #247 a run is two transactions; use {@link #run(UUID)}.
	 */
	@Deprecated
	public void runInNewTransaction(UUID runId) {
		run(runId);
	}

	/**
	 * Outcome of the analyze phase: either a skip reason, or one {@link Step} per assistant run.
	 * {@link #collect} turns the steps into the results to apply once every staged call is done.
	 */
	private static final class Analysis {
		final String skipReason;
		final AssistantContext context;
		/** #363: one per matching assistant, in order. */
		final List<Step> steps;
		/** #260: the definitions behind the assistants that were run. */
		final List<AssistantDefinition> definitions;
		List<RequelAssistant<?>> assistants = List.of();
		List<AssistantResult> results = List.of();
		/** #268: assistants that threw or returned an incomplete result, for the run record. */
		List<String> problems = List.of();
		/** #259: how many matching assistants threw (an incomplete result does not count). */
		int thrownCount;

		Analysis(String skipReason) {
			this.skipReason = skipReason;
			this.context = null;
			this.steps = List.of();
			this.definitions = List.of();
		}

		Analysis(AssistantContext context, List<Step> steps, List<AssistantDefinition> definitions) {
			this.skipReason = null;
			this.context = context;
			this.steps = steps;
			this.definitions = definitions;
		}

		/** Every assistant that ran threw: a failed run, not a partial one. */
		boolean allFailed() {
			return !steps.isEmpty() && thrownCount == steps.size();
		}

		/**
		 * The results in assistant order, each attributed to the assistant that produced it (a
		 * composed pass's results to their members), and the problems.
		 */
		void collect() {
			List<RequelAssistant<?>> producers = new ArrayList<>();
			List<AssistantResult> produced = new ArrayList<>();
			List<String> found = new ArrayList<>();
			int thrown = 0;
			for (Step step : steps) {
				if (step.failure != null) {
					found.add(step.assistant.assistantId() + " failed: " + step.failure);
					thrown++;
					continue;
				}
				for (AssistantResult result : step.results) {
					RequelAssistant<?> producer = step.assistant;
					if (step.assistant instanceof ComposedAssistant composed) {
						RequelAssistant<?> member = composed.memberFor(result.assistantId());
						producer = member == null ? step.assistant : member;
					}
					produced.add(result);
					producers.add(producer);
					if (CommandBackedAssistantResultApplicator.isIncomplete(result)) {
						found.add(producer.assistantId() + " incomplete (failed: "
								+ result.metadata().get("failedProperties") + ")");
					}
				}
			}
			this.assistants = producers;
			this.results = produced;
			this.problems = found;
			this.thrownCount = thrown;
		}
	}

	/**
	 * Issue #363: one assistant's part of a run. A plain assistant has its results once the analyze
	 * phase is over; a {@link StagedAssistant} has its {@link #stage}, completed after it.
	 */
	private static final class Step {
		final RequelAssistant<?> assistant;
		StagedAssistant.Stage stage;
		List<AssistantResult> results = List.of();
		/** What the assistant threw, or null. */
		Exception failure;

		Step(RequelAssistant<?> assistant) {
			this.assistant = assistant;
		}
	}

	private Analysis analyze(AssistantRunRecord record) {
		AnalysisRequest request = record.request();
		Optional<Object> targetValue = loadTarget(request);
		if (targetValue.isEmpty()) {
			return new Analysis("No assistant target loader resolved "
					+ request.targetRef().entityType());
		}

		Object target = targetValue.get();
		// Issue #270: fingerprint the target as this run analyzes it, in this transaction. The
		// apply runs in another one, and an edit in between must leave the findings stale.
		Map<String, Object> attributes = new HashMap<>(request.attributes());
		String fingerprint = TargetFingerprint.of(target);
		if (fingerprint != null) {
			attributes.put(CommandBackedAssistantResultApplicator.TARGET_FINGERPRINT, fingerprint);
		}
		AssistantContext context = new AssistantContext(record.runId(), request.triggeringUser(),
				request.assistantUser(), request.projectRef(), request.taskType(),
				request.locale(), clock, attributes);
		List<RequelAssistant<?>> matched = assistantRegistry.findAssistantsFor(target, context);
		if (matched.isEmpty()) {
			return new Analysis("No assistants registered for " + target.getClass().getName());
		}
		// Route by task type: an assistant runs only if it serves this run's task
		// (default serves the null/post-edit task; e.g. REQUIREMENTS_REVIEW routes to the
		// AI assistant and not the lexical ones, and edits do not trigger the AI assistant).
		List<RequelAssistant<?>> assistants = new ArrayList<>();
		for (RequelAssistant<?> candidate : matched) {
			if (candidate.handlesTask(context.taskType())) {
				assistants.add(candidate);
			}
		}
		if (assistants.isEmpty()) {
			return new Analysis("No assistant handles task "
					+ (context.taskType() == null ? "<default>" : context.taskType()) + " for "
					+ target.getClass().getName());
		}

		// Isolate per-assistant failures: one assistant erroring must not abort the
		// others or fail the whole run (mirrors the legacy per-check resilience). A
		// failed assistant contributes no result. Infrastructure failures (target
		// reload, registry) still fail the run via the caller's catch.
		//
		// #363: a staged assistant only prepares here; its call is made once this transaction has
		// committed (completeStages), so the run holds no connection while a model works.
		List<Step> steps = new ArrayList<>(assistants.size());
		for (RequelAssistant<?> assistant : assistants) {
			Step step = new Step(assistant);
			steps.add(step);
			try {
				if (assistant instanceof StagedAssistant staged) {
					step.stage = staged.prepare(context, target);
				} else if (assistant instanceof ComposedAssistant composed) {
					// #265: a composed assistant reports one result per member it ran for.
					step.results = composed.analyzeAll(context, target);
				} else {
					step.results = List.of(analyze(assistant, context, target));
				}
			} catch (RuntimeException | AssistantException e) {
				failed(record, step, e);
			}
		}
		List<AssistantDefinition> definitions = new ArrayList<>();
		for (RequelAssistant<?> assistant : assistants) {
			List<RequelAssistant<?>> units = assistant instanceof ComposedAssistant composed
					? composed.members() : List.of(assistant);
			for (RequelAssistant<?> unit : units) {
				if (unit instanceof DefinitionBacked backed && backed.definition() != null) {
					definitions.add(backed.definition());
				}
			}
		}
		return new Analysis(context, steps, List.copyOf(definitions));
	}

	/**
	 * Issue #363: make each staged assistant's call. Runs between the two phases, with no
	 * transaction open; a stage touches no entity, so nothing here needs a connection.
	 */
	private void completeStages(AssistantRunRecord record, Analysis analysis) {
		for (Step step : analysis.steps) {
			if (step.stage == null) {
				continue;
			}
			try {
				step.results = List.copyOf(step.stage.complete());
			} catch (RuntimeException | AssistantException e) {
				failed(record, step, e);
			}
		}
	}

	private static void failed(AssistantRunRecord record, Step step, Exception e) {
		log.warn("assistant {} failed for run {}: {}", step.assistant.assistantId(),
				record.runId(), e.toString(), e);
		step.failure = e;
		step.results = List.of();
	}

	/** What the apply phase decided. */
	private static final class Outcome {
		final AssistantRunStatus status;
		final String reason;
		/** #268: assistants whose result failed to apply. */
		final List<String> problems;

		private Outcome(AssistantRunStatus status, String reason, List<String> problems) {
			this.status = status;
			this.reason = reason;
			this.problems = problems;
		}

		static Outcome applied(List<String> problems) {
			return new Outcome(AssistantRunStatus.SUCCEEDED, null, problems);
		}

		static Outcome skipped(String reason) {
			return new Outcome(AssistantRunStatus.SKIPPED, reason, List.of());
		}

		static Outcome cancelled(String reason) {
			return new Outcome(AssistantRunStatus.CANCELLED, reason, List.of());
		}
	}

	/**
	 * @return what happened: applied, skipped (nothing to do), or cancelled (there was
	 *         something to do and it was deliberately dropped - see #279).
	 */
	private Outcome apply(AssistantRunRecord record, Analysis analysis) {
		AnalysisRequest request = record.request();

		// Gate on the project row first. This single statement both takes the lock that
		// orders us against DeleteProjectCommandImpl (which takes the same lock before it
		// touches any child, so neither path can deadlock the other) and answers whether
		// the project still exists. Holding it for the rest of this transaction is what
		// makes the check below meaningful: without the lock, the project could be deleted
		// between the check and the inserts.
		//
		// projectRef is nullable - AnalysisRequest null-checks every other field, and
		// AnalysisRequestDispatcher passes null when the target has no ProjectOrDomain.
		// With no project there is nothing to gate on, so fall through to the pre-#279
		// behaviour: the target-only re-check below.
		EntityRef projectRef = request.projectRef();
		if (projectRef != null) {
			AssistantProjectGate.State gate = projectGate.acquire(projectRef);
			if (gate == AssistantProjectGate.State.GONE) {
				return Outcome.cancelled("Project#" + projectRef.entityId()
						+ " was deleted while the analysis ran; findings discarded");
			}
			if (gate == AssistantProjectGate.State.BUSY) {
				return Outcome.cancelled("Project#" + projectRef.entityId()
						+ " could not be locked before applying; findings discarded");
			}
		}

		if (loadTarget(request).isEmpty()) {
			return Outcome.skipped("Target " + request.targetRef().entityType() + "#"
					+ request.targetRef().entityId() + " no longer exists; findings discarded");
		}
		List<String> problems = new ArrayList<>();
		for (int i = 0; i < analysis.results.size(); i++) {
			RequelAssistant<?> assistant = analysis.assistants.get(i);
			try {
				resultApplicator.apply(analysis.context, analysis.results.get(i),
						assistant.cleanupPolicy(), request.targetRef());
			} catch (RuntimeException e) {
				log.warn("applying assistant {} result failed for run {}: {}",
						assistant.assistantId(), record.runId(), e.toString(), e);
				problems.add(assistant.assistantId() + " apply failed: " + e);
			}
		}
		return Outcome.applied(problems);
	}

	private Optional<Object> loadTarget(AnalysisRequest request) {
		for (AssistantTargetLoader targetLoader : targetLoaders) {
			if (targetLoader.supports(request.targetRef())) {
				return targetLoader.loadTarget(request.targetRef());
			}
		}
		return Optional.empty();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private AssistantResult analyze(RequelAssistant assistant, AssistantContext context,
			Object target) throws AssistantException {
		return assistant.analyze(context, target);
	}
}
