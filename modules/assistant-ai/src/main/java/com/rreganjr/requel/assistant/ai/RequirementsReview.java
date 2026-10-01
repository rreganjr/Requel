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

/**
 * Issue #260: the constants of the default AI requirements review, now that the review itself
 * is the bundled definition {@code ai/definitions/requirements-review.json} run by
 * {@link DefinitionExecutorAssistant}. The key stays {@code ai-requirements-review}: every AI
 * finding's idempotency key starts with it.
 */
public final class RequirementsReview {

	/** The bundled default definition's key, and so its executor's assistant id. */
	public static final String ASSISTANT_ID = "ai-requirements-review";

	public static final String TASK_TYPE = "REQUIREMENTS_REVIEW";

	public static final String OUTPUT_SCHEMA_NAME = "RequirementsReviewOutput";

	public static final String OUTPUT_SCHEMA_VERSION = "1";

	private RequirementsReview() {
	}
}
