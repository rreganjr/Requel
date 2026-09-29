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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.AssistantRunHandle;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;

class AssistantDispatcherImplTest {

	@Test
	void dispatchQueuesRunAndInvokesWorkerThroughExecutor() throws Exception {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of()), new NoOpAssistantResultApplicator(),
				List.of());
		AssistantDispatcherImpl dispatcher = new AssistantDispatcherImpl(new SyncTaskExecutor(),
				runStore, worker);

		AssistantRunHandle handle = dispatcher.dispatch(request()).toCompletableFuture().get();

		assertThat(runStore.findRun(handle.runId())).hasValueSatisfying(record -> {
			assertThat(record.status()).isEqualTo(AssistantRunStatus.SKIPPED);
			assertThat(record.errorSummary()).contains("No assistant target loader");
		});
	}

	@Test
	void dispatchAllQueuesEveryRunAndSubmitsOneTaskThatRunsThemInOrder() throws Exception {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		RecordingWorker worker = new RecordingWorker(runStore, null);
		AtomicInteger submits = new AtomicInteger();
		TaskExecutor executor = task -> {
			submits.incrementAndGet();
			task.run();
		};
		AssistantDispatcherImpl dispatcher = new AssistantDispatcherImpl(executor, runStore, worker);

		List<CompletionStage<AssistantRunHandle>> stages = dispatcher
				.dispatchAll(List.of(request(1L), request(2L), request(3L)));

		assertThat(submits).hasValue(1);
		assertThat(stages).hasSize(3);
		List<UUID> handles = new ArrayList<>();
		for (CompletionStage<AssistantRunHandle> stage : stages) {
			handles.add(stage.toCompletableFuture().get().runId());
		}
		assertThat(worker.ran).containsExactlyElementsOf(handles);
		assertThat(handles).allSatisfy(id -> assertThat(runStore.findRun(id)).isPresent());
	}

	@Test
	void dispatchAllKeepsGoingWhenOneRunThrows() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		RecordingWorker worker = new RecordingWorker(runStore, 0);
		AssistantDispatcherImpl dispatcher = new AssistantDispatcherImpl(new SyncTaskExecutor(),
				runStore, worker);

		dispatcher.dispatchAll(List.of(request(1L), request(2L)));

		assertThat(worker.ran).hasSize(2);
	}

	@Test
	void dispatchAllMarksEveryRunFailedWhenTheExecutorRejectsTheBatch() {
		RecordingRunStore runStore = new RecordingRunStore();
		RecordingWorker worker = new RecordingWorker(runStore, null);
		TaskExecutor rejecting = task -> {
			throw new TaskRejectedException("queue full");
		};
		AssistantDispatcherImpl dispatcher = new AssistantDispatcherImpl(rejecting, runStore, worker);

		List<CompletionStage<AssistantRunHandle>> stages = dispatcher
				.dispatchAll(List.of(request(1L), request(2L)));

		assertThat(stages).hasSize(2).allSatisfy(stage -> assertThat(stage.toCompletableFuture())
				.isCompletedExceptionally());
		assertThat(worker.ran).isEmpty();
		assertThat(runStore.findRun(runStore.queued.get(0))).hasValueSatisfying(
				record -> assertThat(record.status()).isEqualTo(AssistantRunStatus.FAILED));
		assertThat(runStore.findRun(runStore.queued.get(1))).hasValueSatisfying(
				record -> assertThat(record.status()).isEqualTo(AssistantRunStatus.FAILED));
	}

	@Test
	void dispatchAllOfNothingSubmitsNothing() {
		AtomicInteger submits = new AtomicInteger();
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantDispatcherImpl dispatcher = new AssistantDispatcherImpl(
				task -> submits.incrementAndGet(), runStore, new RecordingWorker(runStore, null));

		assertThat(dispatcher.dispatchAll(List.of())).isEmpty();
		assertThat(submits).hasValue(0);
	}

	/** Remembers the ids of the runs it queued. */
	private static final class RecordingRunStore extends InMemoryAssistantRunStore {
		private final List<UUID> queued = new ArrayList<>();

		@Override
		public AssistantRunRecord queueRun(AnalysisRequest request) {
			AssistantRunRecord record = super.queueRun(request);
			queued.add(record.runId());
			return record;
		}
	}

	/** Records the run ids it is handed; optionally throws on one of them. */
	private static final class RecordingWorker extends AssistantRunWorker {
		private final List<UUID> ran = new ArrayList<>();
		private final Integer throwOnIndex;

		RecordingWorker(InMemoryAssistantRunStore runStore, Integer throwOnIndex) {
			super(runStore, new SimpleAssistantRegistry(List.of()),
					new NoOpAssistantResultApplicator(), List.of());
			this.throwOnIndex = throwOnIndex;
		}

		@Override
		public void run(UUID runId) {
			ran.add(runId);
			if (throwOnIndex != null && ran.size() - 1 == throwOnIndex) {
				throw new IllegalStateException("boom");
			}
		}
	}

	private AnalysisRequest request() {
		return request(1L);
	}

	private AnalysisRequest request(long goalId) {
		return new AnalysisRequest(EntityRef.of("Goal", goalId), EntityRef.of("Project", 2L),
				new UserRef(3L, "human"), new UserRef(4L, "assistant"),
				"REQUIREMENTS_REVIEW", java.util.Locale.ROOT, Map.of());
	}
}
