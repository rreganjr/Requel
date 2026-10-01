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
package com.rreganjr.requel.assistant.core.context;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Bookkeeping that travels with a context pack. Built by the builders to give
 * downstream code (logging, observability, AI provider request capping) a
 * cheap, structured view of what's inside the pack without re-walking its
 * snapshots.
 *
 * @param builtAt          when the pack was assembled
 * @param totalCharacters  sum of text-field lengths across all snapshots; used
 *                         by the size-limit gate
 * @param truncated        true if at least one field was clamped to satisfy the
 *                         configured size limit
 * @param redactedFields   field paths that were dropped or replaced by the
 *                         {@link RedactionPolicy}; useful for audit
 * @param truncationNotes  per-field human-readable notes about what got clamped
 */
public record ContextPackMetadata(Instant builtAt, int totalCharacters, boolean truncated,
		List<String> redactedFields, List<String> truncationNotes) {

	public ContextPackMetadata {
		Objects.requireNonNull(builtAt, "builtAt");
		redactedFields = redactedFields == null ? List.of() : List.copyOf(redactedFields);
		truncationNotes = truncationNotes == null ? List.of() : List.copyOf(truncationNotes);
	}

	/** Issue #262: how many values were masked, read from the policy's notes. */
	public int redactionCount() {
		int total = 0;
		for (String note : redactedFields) {
			java.util.regex.Matcher m = REDACTION_NOTE.matcher(note);
			while (m.find()) {
				total += Integer.parseInt(m.group(2));
			}
		}
		return total;
	}

	/** Issue #262: the categories masked, in first-seen order, read from the policy's notes. */
	public List<String> redactionCategories() {
		java.util.LinkedHashSet<String> categories = new java.util.LinkedHashSet<>();
		for (String note : redactedFields) {
			java.util.regex.Matcher m = REDACTION_NOTE.matcher(note);
			while (m.find()) {
				categories.add(m.group(1));
			}
		}
		return List.copyOf(categories);
	}

	/** {@code EMAIL x2} as written by {@link DefaultRedactionPolicy}. */
	private static final java.util.regex.Pattern REDACTION_NOTE = java.util.regex.Pattern
			.compile("\\b([A-Z][A-Z_]*) x(\\d+)\\b");

	public static ContextPackMetadata empty(Instant now) {
		return new ContextPackMetadata(now, 0, false, List.of(), List.of());
	}
}
