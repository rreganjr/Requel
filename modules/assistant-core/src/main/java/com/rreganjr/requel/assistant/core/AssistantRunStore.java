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

	void markSkipped(UUID runId, String reason);

	void markFailed(UUID runId, Throwable failure);

	Optional<AssistantRunRecord> findRun(UUID runId);
}
