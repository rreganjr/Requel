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
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.rreganjr.requel.assistant.api.AnalysisRequest;

/**
 * In-memory {@link AssistantRunStore} kept for unit tests that don't load a
 * Spring context. The production wiring is
 * {@link com.rreganjr.requel.assistant.core.persistence.JpaAssistantRunStore};
 * this class is no longer a {@code @Component} since Phase 2.
 */
public class InMemoryAssistantRunStore implements AssistantRunStore {

	private final Clock clock;
	private final Map<UUID, AssistantRunRecord> runs = new ConcurrentHashMap<UUID, AssistantRunRecord>();

	public InMemoryAssistantRunStore() {
		this(Clock.systemUTC());
	}

	InMemoryAssistantRunStore(Clock clock) {
		this.clock = clock;
	}

	@Override
	public AssistantRunRecord queueRun(AnalysisRequest request) {
		UUID runId = UUID.randomUUID();
		Instant now = clock.instant();
		AssistantRunRecord record = new AssistantRunRecord(runId, request,
				AssistantRunStatus.QUEUED, now, now, null);
		runs.put(runId, record);
		return record;
	}

	@Override
	public void markRunning(UUID runId) {
		update(runId, AssistantRunStatus.RUNNING, null);
	}

	@Override
	public void markSucceeded(UUID runId) {
		update(runId, AssistantRunStatus.SUCCEEDED, null);
	}

	@Override
	public void markSkipped(UUID runId, String reason) {
		update(runId, AssistantRunStatus.SKIPPED, reason);
	}

	@Override
	public void markFailed(UUID runId, Throwable failure) {
		update(runId, AssistantRunStatus.FAILED, failure.getMessage());
	}

	@Override
	public Optional<AssistantRunRecord> findRun(UUID runId) {
		return Optional.ofNullable(runs.get(runId));
	}

	private void update(UUID runId, AssistantRunStatus status, String errorSummary) {
		runs.computeIfPresent(runId, (id, record) -> record.withStatus(status, clock.instant(),
				errorSummary));
	}
}
