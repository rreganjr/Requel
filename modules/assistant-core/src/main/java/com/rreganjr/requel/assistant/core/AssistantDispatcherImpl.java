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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.AssistantDispatcher;
import com.rreganjr.requel.assistant.api.AssistantRunHandle;

/**
 * Queues assistant work and hands execution to {@link AssistantRunWorker}.
 * This class intentionally does not hold a transaction or touch entity state.
 */
@Component
public class AssistantDispatcherImpl implements AssistantDispatcher {

	private static final Logger log = LoggerFactory.getLogger(AssistantDispatcherImpl.class);

	private final TaskExecutor taskExecutor;
	private final AssistantRunStore runStore;
	private final AssistantRunWorker runWorker;

	@Autowired
	public AssistantDispatcherImpl(@Qualifier("assistantTaskExecutor") TaskExecutor taskExecutor,
			AssistantRunStore runStore, AssistantRunWorker runWorker) {
		this.taskExecutor = taskExecutor;
		this.runStore = runStore;
		this.runWorker = runWorker;
	}

	@Override
	public CompletionStage<AssistantRunHandle> dispatch(AnalysisRequest request) {
		Objects.requireNonNull(request, "request");
		AssistantRunRecord record = runStore.queueRun(request);
		if (deferUntilCommit(() -> submitOne(record))) {
			return CompletableFuture.completedFuture(new AssistantRunHandle(record.runId()));
		}
		try {
			submitOne(record);
		} catch (TaskRejectedException e) {
			runStore.markFailed(record.runId(), e);
			return CompletableFuture.failedStage(e);
		}
		return CompletableFuture.completedFuture(new AssistantRunHandle(record.runId()));
	}

	/**
	 * Queues every run, then submits one executor task that runs them in order (#268), so a
	 * whole-project analysis takes one executor slot instead of filling the queue. A run that
	 * throws is logged and the next one still runs. A rejected submit marks every run FAILED.
	 */
	@Override
	public List<CompletionStage<AssistantRunHandle>> dispatchAll(List<AnalysisRequest> requests) {
		Objects.requireNonNull(requests, "requests");
		if (requests.isEmpty()) {
			return List.of();
		}
		List<AssistantRunRecord> records = new ArrayList<>(requests.size());
		for (AnalysisRequest request : requests) {
			records.add(runStore.queueRun(Objects.requireNonNull(request, "request")));
		}
		if (deferUntilCommit(() -> submitBatch(records))) {
			List<CompletionStage<AssistantRunHandle>> stages = new ArrayList<>(records.size());
			for (AssistantRunRecord record : records) {
				stages.add(CompletableFuture.completedFuture(new AssistantRunHandle(record.runId())));
			}
			return stages;
		}
		try {
			submitBatch(records);
		} catch (TaskRejectedException e) {
			List<CompletionStage<AssistantRunHandle>> failed = new ArrayList<>(records.size());
			for (AssistantRunRecord record : records) {
				runStore.markFailed(record.runId(), e);
				failed.add(CompletableFuture.failedStage(e));
			}
			return failed;
		}
		List<CompletionStage<AssistantRunHandle>> stages = new ArrayList<>(records.size());
		for (AssistantRunRecord record : records) {
			stages.add(CompletableFuture.completedFuture(new AssistantRunHandle(record.runId())));
		}
		return stages;
	}

	private void submitOne(AssistantRunRecord record) {
		taskExecutor.execute(new Runnable() {
			@Override
			public void run() {
				runWorker.run(record.runId());
			}
		});
	}

	private void submitBatch(List<AssistantRunRecord> records) {
		taskExecutor.execute(new Runnable() {
			@Override
			public void run() {
				for (AssistantRunRecord record : records) {
					try {
						runWorker.run(record.runId());
					} catch (RuntimeException e) {
						log.warn("Assistant run {} failed in a batch of {}: {}", record.runId(),
								records.size(), e.getMessage(), e);
					}
				}
			}
		});
	}

	/**
	 * #259: a dispatch made inside a caller's transaction queues its run row in that transaction,
	 * so a worker started now looks the row up before it is committed, finds nothing, and the run
	 * stays QUEUED forever. When a transaction is active the submit waits for its commit (and never
	 * happens on rollback, which also removes the row). Returns whether it was deferred.
	 */
	private boolean deferUntilCommit(Runnable submit) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return false;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					submit.run();
				} catch (TaskRejectedException e) {
					// The caller's transaction is already committed; the run stays QUEUED.
					log.warn("Assistant run submit rejected after commit: {}", e.getMessage(), e);
				}
			}
		});
		return true;
	}
}
