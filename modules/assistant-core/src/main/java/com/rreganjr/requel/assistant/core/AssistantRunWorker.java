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
import java.util.List;
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
 */
@Component
public class AssistantRunWorker {

	private static final Logger log = LoggerFactory.getLogger(AssistantRunWorker.class);

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
	 * Execute a queued run: analyze in one transaction, apply the findings in a
	 * second short one. Never joins a caller's transaction.
	 *
	 * @param runId
	 *            the queued run.
	 */
	public void run(UUID runId) {
		Objects.requireNonNull(runId, "runId");
		Optional<AssistantRunRecord> recordValue = runStore.findRun(runId);
		if (recordValue.isEmpty()) {
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
				default -> runStore.markSucceeded(runId);
			}
		} catch (RuntimeException e) {
			runStore.markFailed(runId, e);
			throw new AssistantWorkerException("Assistant run failed: " + runId, e);
		}
	}

	/**
	 * @deprecated since #247 a run is two transactions; use {@link #run(UUID)}.
	 */
	@Deprecated
	public void runInNewTransaction(UUID runId) {
		run(runId);
	}

	/** Outcome of the analyze phase: either a skip reason or the results to apply. */
	private static final class Analysis {
		final String skipReason;
		final AssistantContext context;
		final List<RequelAssistant<?>> assistants;
		final List<AssistantResult> results;

		Analysis(String skipReason) {
			this.skipReason = skipReason;
			this.context = null;
			this.assistants = List.of();
			this.results = List.of();
		}

		Analysis(AssistantContext context, List<RequelAssistant<?>> assistants,
				List<AssistantResult> results) {
			this.skipReason = null;
			this.context = context;
			this.assistants = assistants;
			this.results = results;
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
		AssistantContext context = new AssistantContext(record.runId(), request.triggeringUser(),
				request.assistantUser(), request.projectRef(), request.taskType(),
				request.locale(), clock, request.attributes());
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
		List<AssistantResult> results = new ArrayList<>(assistants.size());
		List<RequelAssistant<?>> producers = new ArrayList<>(assistants.size());
		for (RequelAssistant<?> assistant : assistants) {
			try {
				results.add(analyze(assistant, context, target));
				producers.add(assistant);
			} catch (RuntimeException | AssistantException e) {
				log.warn("assistant {} failed for run {}: {}", assistant.assistantId(),
						record.runId(), e.toString(), e);
			}
		}
		return new Analysis(context, producers, results);
	}

	/** What the apply phase decided. */
	private static final class Outcome {
		final AssistantRunStatus status;
		final String reason;

		private Outcome(AssistantRunStatus status, String reason) {
			this.status = status;
			this.reason = reason;
		}

		static Outcome applied() {
			return new Outcome(AssistantRunStatus.SUCCEEDED, null);
		}

		static Outcome skipped(String reason) {
			return new Outcome(AssistantRunStatus.SKIPPED, reason);
		}

		static Outcome cancelled(String reason) {
			return new Outcome(AssistantRunStatus.CANCELLED, reason);
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
		for (int i = 0; i < analysis.results.size(); i++) {
			RequelAssistant<?> assistant = analysis.assistants.get(i);
			try {
				resultApplicator.apply(analysis.context, analysis.results.get(i),
						assistant.cleanupPolicy(), request.targetRef());
			} catch (RuntimeException e) {
				log.warn("applying assistant {} result failed for run {}: {}",
						assistant.assistantId(), record.runId(), e.toString(), e);
			}
		}
		return Outcome.applied();
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
