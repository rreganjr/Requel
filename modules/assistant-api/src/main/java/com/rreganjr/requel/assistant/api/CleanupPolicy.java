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

/**
 * How an assistant's stale findings are handled when a later run no longer
 * reports them (or reports a changed version of them). Declared per assistant via
 * {@link RequelAssistant#cleanupPolicy()}; the result applicator owns the
 * transitions. See {@code doc/assistant-spi-plan.md} (Finding State Machine).
 */
public enum CleanupPolicy {

	/**
	 * Never auto-close or supersede assistant findings; only the applicator's
	 * {@code DROPPED} (rejected at apply) and human {@code MANUALLY_RESOLVED}
	 * transitions apply. Use for assistants whose findings should persist until a
	 * human acts on them.
	 */
	MANUAL,

	/**
	 * When a later run reports the same logical finding with different evidence,
	 * mark the prior finding {@code SUPERSEDED} (annotation left open, a system
	 * position noting the supersession is posted). Findings simply omitted by a
	 * later run are left untouched. This is the default.
	 */
	MARK_SUPERSEDED,

	/**
	 * In addition to {@link #MARK_SUPERSEDED} behavior, when a later run omits a
	 * finding entirely and the linked annotation has no human edits, replies, or
	 * non-assistant positions, close the annotation and mark the finding
	 * {@code AUTO_RESOLVED}.
	 */
	AUTO_RESOLVE_IF_UNTOUCHED
}
