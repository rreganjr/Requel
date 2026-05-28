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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;

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

	private AnalysisRequest request() {
		return new AnalysisRequest(EntityRef.of("Goal", 1L), EntityRef.of("Project", 2L),
				new UserRef(3L, "human"), new UserRef(4L, "assistant"),
				"REQUIREMENTS_REVIEW", Map.of());
	}
}
