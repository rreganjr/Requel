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
package com.rreganjr.requel.assistant.ai;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

public record AiUsage(String provider, String model, Integer inputTokens, Integer outputTokens,
		Integer cachedInputTokens, Duration latency, BigDecimal costEstimate) {

	public AiUsage {
		Objects.requireNonNull(provider, "provider");
		Objects.requireNonNull(model, "model");
		Objects.requireNonNull(latency, "latency");
	}

	public static AiUsage noop(String model, Duration latency) {
		return new AiUsage("noop", model, 0, 0, 0, latency, BigDecimal.ZERO);
	}
}
