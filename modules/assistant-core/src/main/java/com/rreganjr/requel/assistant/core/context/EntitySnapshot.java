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

/**
 * Sealed parent of every per-entity snapshot carried in a context pack.
 *
 * <p>Each implementer is an immutable record tuned for AI input: only the
 * fields an assistant needs to reason about the entity, with version stamps
 * so {@link com.rreganjr.requel.assistant.core.AssistantResultApplicator}
 * can reject stale AI output by comparing the persisted entity's optimistic
 * lock against the snapshot version it produced findings against.</p>
 */
public sealed interface EntitySnapshot
		permits ActorSnapshot, GoalSnapshot, StorySnapshot, ScenarioSnapshot, StepSnapshot,
				UseCaseSnapshot, GlossaryTermSnapshot, ProjectSnapshot {

	Long id();

	int version();
}
