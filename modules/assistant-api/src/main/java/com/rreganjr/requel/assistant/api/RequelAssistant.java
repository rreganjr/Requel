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
 * Stable SPI implemented by legacy, rules-based, and AI-backed assistants.
 *
 * @param <T>
 *            target domain interface type handled by the assistant
 */
public interface RequelAssistant<T> {

	String assistantId();

	Class<T> targetType();

	AssistantResult analyze(AssistantContext context, T target) throws AssistantException;

	/**
	 * Whether this assistant serves the given run task type. The worker invokes an
	 * assistant only when this returns {@code true}, so different task types route to
	 * different assistants even though {@code SimpleAssistantRegistry} matches by target type.
	 *
	 * <p>
	 * The default serves the ordinary post-edit analysis, identified by a {@code null} task
	 * type (the path the legacy/NLP adapters handle). Task-specific assistants — e.g. the AI
	 * requirements review ({@code "REQUIREMENTS_REVIEW"}) — override this so they run only for
	 * their task and not on ordinary edits, and so ordinary edits do not trigger them.
	 *
	 * @param taskType the run's task type, or {@code null} for the default post-edit analysis
	 */
	default boolean handlesTask(String taskType) {
		return taskType == null;
	}

	/**
	 * The stale-finding cleanup policy for this assistant. Defaults to
	 * {@link CleanupPolicy#MARK_SUPERSEDED}; override to opt into auto-resolution
	 * or to disable automatic cleanup. The result applicator reads this when
	 * reconciling a run's findings against previously recorded ones.
	 */
	default CleanupPolicy cleanupPolicy() {
		return CleanupPolicy.MARK_SUPERSEDED;
	}
}
