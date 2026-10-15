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
package com.rreganjr.requel.annotation;

import java.util.Locale;
import java.util.Optional;

/**
 * How much an {@link Issue} matters, so a reviewer (or an MCP client) can read the findings that
 * count before the noise. A small closed vocabulary, the same one the AI review output schema uses
 * ({@code requirements-review-output.v1.json}). {@code mustBeResolved} is a separate gate, not a
 * priority, and does not enter the ordering.
 *
 * <p>
 * Persisted by name ({@code annotations.severity}, a {@code VARCHAR}), so adding a value later is
 * not a column change. Issue #271.
 */
public enum IssueSeverity {
	LOW(1), MEDIUM(2), HIGH(3);

	private final int rank;

	IssueSeverity(int rank) {
		this.rank = rank;
	}

	/**
	 * @return the ordering weight; a higher rank lists first.
	 */
	public int rank() {
		return rank;
	}

	/**
	 * Case-insensitive, surrounding whitespace ignored.
	 *
	 * @param value
	 *            a severity name such as {@code "high"}
	 * @return the severity, or empty for a null, blank or unrecognised value
	 */
	public static Optional<IssueSeverity> parse(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		String name = value.trim().toUpperCase(Locale.ROOT);
		for (IssueSeverity severity : values()) {
			if (severity.name().equals(name)) {
				return Optional.of(severity);
			}
		}
		return Optional.empty();
	}

	/**
	 * @return the rank of the given severity, 0 for null, so a list sorted by descending rank puts
	 *         anything without a severity last.
	 */
	public static int rankOf(IssueSeverity severity) {
		return severity == null ? 0 : severity.rank();
	}
}
