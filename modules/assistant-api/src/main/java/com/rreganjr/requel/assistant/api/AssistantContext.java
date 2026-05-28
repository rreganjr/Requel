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

import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Per-run assistant context. It carries stable references and capabilities
 * rather than persistence entities.
 */
public record AssistantContext(UUID runId, UserRef triggeringUser, UserRef assistantUser,
		EntityRef projectRef, Locale locale, Clock clock, Map<String, Object> attributes) {

	public AssistantContext {
		Objects.requireNonNull(runId, "runId");
		Objects.requireNonNull(triggeringUser, "triggeringUser");
		Objects.requireNonNull(assistantUser, "assistantUser");
		Objects.requireNonNull(locale, "locale");
		Objects.requireNonNull(clock, "clock");
		attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
	}
}
