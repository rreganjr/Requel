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

import java.util.Optional;
import java.util.UUID;

import com.rreganjr.requel.assistant.api.AnalysisRequest;

/**
 * Storage abstraction for assistant run state.
 */
public interface AssistantRunStore {

	AssistantRunRecord queueRun(AnalysisRequest request);

	void markRunning(UUID runId);

	void markSucceeded(UUID runId);

	/**
	 * Issue #268: the run succeeded, but an assistant threw, or returned an incomplete result,
	 * or its result failed to apply. The other assistants' results are real, so the status is
	 * {@code SUCCEEDED}; {@code error_kind} is {@code PARTIAL} and {@code summary} names the
	 * assistants.
	 */
	void markPartial(UUID runId, String summary);

	void markSkipped(UUID runId, String reason);

	/**
	 * Terminal state for a run whose findings were deliberately thrown away because the
	 * world moved on while it was analyzing - the project was deleted, or its row could not
	 * be locked in time (issue #279).
	 * <p>
	 * Distinct from {@link #markSkipped(UUID, String)}, which means there was nothing to do
	 * in the first place (no assistant matched, no target loader, nothing to analyze). A
	 * cancelled run <em>had</em> results and dropped them; that difference matters when
	 * reading run history to explain missing annotations.
	 */
	void markCancelled(UUID runId, String reason);

	void markFailed(UUID runId, Throwable failure);

	/**
	 * Issue #262: record what the redaction policy masked in the run's context. Best effort; a
	 * store that cannot record it ignores the call.
	 *
	 * @param categories the categories masked, in first-seen order
	 */
	default void recordRedactions(UUID runId, int count, java.util.List<String> categories) {
	}

	Optional<AssistantRunRecord> findRun(UUID runId);
}
