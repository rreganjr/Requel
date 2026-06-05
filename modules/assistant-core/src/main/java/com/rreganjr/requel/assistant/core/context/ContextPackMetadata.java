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

	public static ContextPackMetadata empty(Instant now) {
		return new ContextPackMetadata(now, 0, false, List.of(), List.of());
	}
}
