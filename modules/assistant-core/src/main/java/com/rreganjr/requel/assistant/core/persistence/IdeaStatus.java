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
package com.rreganjr.requel.assistant.core.persistence;

/**
 * Where an idea is in generating its drafts. The names are stored in {@code idea_generations.status} and
 * written as literals in {@link IdeaRepository}'s claim query, so keep them in step.
 */
public enum IdeaStatus {
	/** Saved, generation queued. */
	PENDING,
	/** A generation attempt is running. */
	GENERATING,
	/** Drafts were generated and stored. */
	DRAFTED,
	/** The attempt failed; {@code errorSummary} says why. Retryable. */
	FAILED,
	/** No call was made (AI off for the project, input over the cap). Retryable. */
	SKIPPED
}
