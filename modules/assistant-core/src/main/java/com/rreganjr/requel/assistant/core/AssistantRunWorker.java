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
 */
@Component
public class AssistantRunWorker {

	private static final Logger log = LoggerFactory.getLogger(AssistantRunWorker.class);

	private final AssistantRunStore runStore;
	private final AssistantRegistry assistantRegistry;
	private final AssistantResultApplicator resultApplicator;
	private final List<AssistantTargetLoader> targetLoaders;
	private final Clock clock;
	private final TransactionOperations analyzeTransaction;
	private final TransactionOperations applyTransaction;

	@Autowired
	public AssistantRunWorker(AssistantRunStore runStore, AssistantRegistry assistantRegistry,
			AssistantResultApplicator resultApplicator, List<AssistantTargetLoader> targetLoaders,
			PlatformTransactionManager transactionManager) {
		this(runStore, assistantRegistry, resultApplicator, targetLoaders, Clock.systemUTC(),
				requiresNew(transactionManager), requiresNew(transactionManager));
	}

	/**
	 * Test constructor: both phases run inline, without a transaction manager.
	 */
	AssistantRunWorker(AssistantRunStore runStore, AssistantRegistry assistantRegistry,
			AssistantResultApplicator resultApplicator, List<AssistantTargetLoader> targetLoaders) {
		this(runStore, assistantRegistry, resultApplicator, targetLoaders, Clock.systemUTC());
	}

	/**
	 * Test constructor: both phases run inline, without a transaction manager.
	 */
	AssistantRunWorker(AssistantRunStore runStore, AssistantRegistry assistantRegistry,
			AssistantResultApplicator resultApplicator, List<AssistantTargetLoader> targetLoaders,
			Clock clock) {
		this(runStore, assistantRegistry, resultApplicator, targetLoaders, clock,
				TransactionOperations.withoutTransaction(), TransactionOperations.withoutTransaction());
	}

	AssistantRunWorker(AssistantRunStore runStore, AssistantRegistry assistantRegistry,
			AssistantResultApplicator resultApplicator, List<AssistantTargetLoader> targetLoaders,
			Clock clock, TransactionOperations analyzeTransaction,
			TransactionOperations applyTransaction) {
		this.runStore = runStore;
		this.assistantRegistry = assistantRegistry;
		this.resultApplicator = resultApplicator;
		this.targetLoaders = List.copyOf(targetLoaders);
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

			// Phase 2 - apply, briefly. Re-check the target: the user may have deleted it
			// while phase 1 ran, in which case writing findings would only produce FK
			// violations against a row that is gone.
			String applySkipReason = applyTransaction.execute(status -> apply(record, analysis));
			if (applySkipReason != null) {
				runStore.markSkipped(runId, applySkipReason);
				return;
			}
			runStore.markSucceeded(runId);
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

	/**
	 * @return a skip reason, or {@code null} when the results were applied.
	 */
	private String apply(AssistantRunRecord record, Analysis analysis) {
		AnalysisRequest request = record.request();
		if (loadTarget(request).isEmpty()) {
			return "Target " + request.targetRef().entityType() + "#"
					+ request.targetRef().entityId() + " no longer exists; findings discarded";
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
		return null;
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
