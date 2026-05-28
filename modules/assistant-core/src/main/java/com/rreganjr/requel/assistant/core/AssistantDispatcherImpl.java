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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.AssistantDispatcher;
import com.rreganjr.requel.assistant.api.AssistantRunHandle;

/**
 * Queues assistant work and hands execution to {@link AssistantRunWorker}.
 * This class intentionally does not hold a transaction or touch entity state.
 */
@Component
public class AssistantDispatcherImpl implements AssistantDispatcher {

	private final TaskExecutor taskExecutor;
	private final AssistantRunStore runStore;
	private final AssistantRunWorker runWorker;

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
		try {
			taskExecutor.execute(new Runnable() {
				@Override
				public void run() {
					runWorker.runInNewTransaction(record.runId());
				}
			});
		} catch (TaskRejectedException e) {
			runStore.markFailed(record.runId(), e);
			return CompletableFuture.failedStage(e);
		}
		return CompletableFuture.completedFuture(new AssistantRunHandle(record.runId()));
	}
}
