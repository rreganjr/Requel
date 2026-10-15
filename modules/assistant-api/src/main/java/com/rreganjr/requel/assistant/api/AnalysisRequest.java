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
package com.rreganjr.requel.assistant.api;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Request to run assistants for a target.
 *
 * <p>
 * {@code locale} is carried on the request (rather than defaulted on the worker
 * thread) so per-run locale is preserved end-to-end. Construct it from the
 * triggering user's locale; use {@link Locale#ROOT} when no locale applies.
 */
public record AnalysisRequest(EntityRef targetRef, EntityRef projectRef, UserRef triggeringUser,
		UserRef assistantUser, String taskType, Locale locale, Map<String, Object> attributes) {

	public AnalysisRequest {
		Objects.requireNonNull(targetRef, "targetRef");
		Objects.requireNonNull(triggeringUser, "triggeringUser");
		Objects.requireNonNull(assistantUser, "assistantUser");
		Objects.requireNonNull(locale, "locale");
		attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
	}

	public Optional<String> taskTypeValue() {
		return Optional.ofNullable(taskType);
	}
}
