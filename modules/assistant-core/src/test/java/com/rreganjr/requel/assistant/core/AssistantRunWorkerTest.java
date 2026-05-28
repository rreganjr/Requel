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
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.api.UserRef;

class AssistantRunWorkerTest {

	@Test
	void runInNewTransactionReloadsTargetAndAppliesMatchingAssistantResult() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())), applicator,
				List.of(new StringTargetLoader()));

		worker.runInNewTransaction(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SUCCEEDED));
		assertThat(applicator.appliedResults).hasSize(1);
		assertThat(applicator.appliedResults.get(0).assistantId()).isEqualTo("string-assistant");
	}

	private AnalysisRequest request() {
		return new AnalysisRequest(EntityRef.of("Goal", 1L), EntityRef.of("Project", 2L),
				new UserRef(3L, "human"), new UserRef(4L, "assistant"),
				"REQUIREMENTS_REVIEW", Map.of());
	}

	private static final class StringTargetLoader implements AssistantTargetLoader {
		@Override
		public boolean supports(EntityRef targetRef) {
			return "Goal".equals(targetRef.entityType());
		}

		@Override
		public Optional<Object> loadTarget(EntityRef targetRef) {
			return Optional.of("target");
		}
	}

	private static final class StringAssistant implements RequelAssistant<String> {
		@Override
		public String assistantId() {
			return "string-assistant";
		}

		@Override
		public Class<String> targetType() {
			return String.class;
		}

		@Override
		public AssistantResult analyze(AssistantContext context, String target) {
			return AssistantResult.builder().assistantId(assistantId()).runId(context.runId())
					.summary(target).build();
		}
	}

	private static final class RecordingApplicator implements AssistantResultApplicator {
		private final List<AssistantResult> appliedResults = new java.util.ArrayList<AssistantResult>();

		@Override
		public AppliedAssistantResult apply(AssistantContext context, AssistantResult result) {
			appliedResults.add(result);
			return new AppliedAssistantResult(0, List.of());
		}
	}
}
