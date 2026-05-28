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
package com.rreganjr.requel.assistant.core.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.AssistantRunRecord;
import com.rreganjr.requel.assistant.core.AssistantRunStatus;

class JpaAssistantRunStoreTest {

	private final Instant fixedNow = Instant.parse("2026-05-27T12:00:00Z");
	private final Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

	@Test
	void queueRunPersistsEntityWithFieldsFromRequest() {
		AssistantRunRepository repository = mock(AssistantRunRepository.class);
		JpaAssistantRunStore store = new JpaAssistantRunStore(repository, fixedClock);
		AnalysisRequest request = request("REQUIREMENTS_REVIEW", 42L, 7L, 3L, 11L);

		AssistantRunRecord record = store.queueRun(request);

		ArgumentCaptor<AssistantRunEntity> captor = ArgumentCaptor.forClass(AssistantRunEntity.class);
		verify(repository).save(captor.capture());
		AssistantRunEntity entity = captor.getValue();
		assertThat(entity.getRunId()).isEqualTo(record.runId());
		assertThat(entity.getStatus()).isEqualTo("QUEUED");
		assertThat(entity.getAssistantId()).isEqualTo("REQUIREMENTS_REVIEW");
		assertThat(entity.getProjectId()).isEqualTo(7L);
		assertThat(entity.getTargetType()).isEqualTo("Goal");
		assertThat(entity.getTargetId()).isEqualTo(42L);
		assertThat(entity.getTriggeredByUserId()).isEqualTo(3L);
		assertThat(entity.getAssistantUserId()).isEqualTo(11L);
		assertThat(entity.getCreatedAt()).isEqualTo(fixedNow);
		assertThat(entity.getUpdatedAt()).isEqualTo(fixedNow);
		assertThat(entity.getFindingsCount()).isZero();
		assertThat(record.status()).isEqualTo(AssistantRunStatus.QUEUED);
	}

	@Test
	void markRunningSetsStatusAndStartedAt() {
		AssistantRunRepository repository = mock(AssistantRunRepository.class);
		JpaAssistantRunStore store = new JpaAssistantRunStore(repository, fixedClock);
		AssistantRunEntity entity = new AssistantRunEntity(UUID.randomUUID(), "test", "QUEUED",
				fixedNow.minusSeconds(1), fixedNow.minusSeconds(1));
		when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));

		store.markRunning(entity.getRunId());

		assertThat(entity.getStatus()).isEqualTo("RUNNING");
		assertThat(entity.getStartedAt()).isEqualTo(fixedNow);
		assertThat(entity.getUpdatedAt()).isEqualTo(fixedNow);
		verify(repository, times(1)).save(entity);
	}

	@Test
	void markFailedRecordsKindAndSummary() {
		AssistantRunRepository repository = mock(AssistantRunRepository.class);
		JpaAssistantRunStore store = new JpaAssistantRunStore(repository, fixedClock);
		AssistantRunEntity entity = new AssistantRunEntity(UUID.randomUUID(), "test", "RUNNING",
				fixedNow.minusSeconds(2), fixedNow.minusSeconds(1));
		when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));

		store.markFailed(entity.getRunId(), new IllegalStateException("boom"));

		assertThat(entity.getStatus()).isEqualTo("FAILED");
		assertThat(entity.getErrorKind()).isEqualTo("IllegalStateException");
		assertThat(entity.getErrorSummary()).isEqualTo("boom");
		assertThat(entity.getCompletedAt()).isEqualTo(fixedNow);
	}

	@Test
	void findRunRebuildsRequestFromPersistedFields() {
		AssistantRunRepository repository = mock(AssistantRunRepository.class);
		JpaAssistantRunStore store = new JpaAssistantRunStore(repository, fixedClock);
		UUID runId = UUID.randomUUID();
		AssistantRunEntity entity = new AssistantRunEntity(runId, "REQUIREMENTS_REVIEW", "RUNNING",
				fixedNow.minusSeconds(5), fixedNow);
		entity.setProjectId(7L);
		entity.setTargetType("Goal");
		entity.setTargetId(42L);
		entity.setTaskType("REQUIREMENTS_REVIEW");
		entity.setTriggeredByUserId(3L);
		entity.setAssistantUserId(11L);
		when(repository.findById(runId.toString())).thenReturn(Optional.of(entity));

		Optional<AssistantRunRecord> found = store.findRun(runId);

		assertThat(found).isPresent();
		AssistantRunRecord record = found.get();
		assertThat(record.runId()).isEqualTo(runId);
		assertThat(record.status()).isEqualTo(AssistantRunStatus.RUNNING);
		assertThat(record.request().targetRef()).isEqualTo(EntityRef.of("Goal", 42L));
		assertThat(record.request().projectRef()).isEqualTo(EntityRef.of("Project", 7L));
		assertThat(record.request().triggeringUser().userId()).isEqualTo(3L);
		assertThat(record.request().assistantUser().userId()).isEqualTo(11L);
	}

	@Test
	void queueRunSurvivesMissingProjectAndTaskType() {
		AssistantRunRepository repository = mock(AssistantRunRepository.class);
		JpaAssistantRunStore store = new JpaAssistantRunStore(repository, fixedClock);
		// AnalysisRequest requires non-null target / triggering / assistant
		// users; projectRef and taskType may be null.
		AnalysisRequest minimal = new AnalysisRequest(EntityRef.of("Goal", 42L), null,
				new UserRef(3L, "ron"), new UserRef(11L, "assistant"), null, Map.of());

		store.queueRun(minimal);

		ArgumentCaptor<AssistantRunEntity> captor = ArgumentCaptor.forClass(AssistantRunEntity.class);
		verify(repository).save(captor.capture());
		AssistantRunEntity entity = captor.getValue();
		assertThat(entity.getProjectId()).isNull();
		assertThat(entity.getTriggeredByUserId()).isEqualTo(3L);
		assertThat(entity.getAssistantUserId()).isEqualTo(11L);
		// No task type set → assistant id falls back to assistant user name.
		assertThat(entity.getAssistantId()).isEqualTo("assistant");
	}

	@Test
	void markSkippedRecordsReason() {
		AssistantRunRepository repository = mock(AssistantRunRepository.class);
		JpaAssistantRunStore store = new JpaAssistantRunStore(repository, fixedClock);
		AssistantRunEntity entity = new AssistantRunEntity(UUID.randomUUID(), "test", "RUNNING",
				fixedNow.minusSeconds(2), fixedNow.minusSeconds(1));
		when(repository.findById(any())).thenReturn(Optional.of(entity));

		store.markSkipped(entity.getRunId(), "no assistants registered");

		assertThat(entity.getStatus()).isEqualTo("SKIPPED");
		assertThat(entity.getErrorSummary()).isEqualTo("no assistants registered");
		assertThat(entity.getCompletedAt()).isEqualTo(fixedNow);
	}

	private static AnalysisRequest request(String taskType, long goalId, long projectId,
			long triggeringUserId, long assistantUserId) {
		return new AnalysisRequest(EntityRef.of("Goal", goalId), EntityRef.of("Project", projectId),
				new UserRef(triggeringUserId, "ron"), new UserRef(assistantUserId, "assistant"),
				taskType, Map.of());
	}
}
