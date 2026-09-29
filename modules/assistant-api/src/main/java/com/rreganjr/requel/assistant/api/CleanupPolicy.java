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
 * transitions. See {@code doc/work/2.0/assistant-spi-plan.md} (Finding State Machine).
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
	 * When a later run of the assistant on the same entity no longer reports a finding, mark it
	 * {@code SUPERSEDED} and leave its annotation open; the annotation reads stale ("may no
	 * longer apply", issue #270) until a run reports it again or a human acts on it. This is the
	 * default.
	 */
	MARK_SUPERSEDED,

	/**
	 * When a later run no longer reports a finding and its annotation carries no human
	 * discussion (unresolved, and every position and argument on it written by the assistant
	 * user), remove the annotation and mark the finding {@code AUTO_RESOLVED}. When a human has
	 * resolved or discussed it, keep the annotation and mark the finding {@code SUPERSEDED}, as
	 * {@link #MARK_SUPERSEDED} does (issue #270).
	 */
	AUTO_RESOLVE_IF_UNTOUCHED
}
