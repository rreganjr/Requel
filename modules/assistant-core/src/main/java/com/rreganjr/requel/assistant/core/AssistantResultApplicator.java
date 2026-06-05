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
package com.rreganjr.requel.assistant.core;

import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;

/**
 * Applies assistant result actions through existing Requel commands.
 */
public interface AssistantResultApplicator {

	/**
	 * Apply a result and reconcile the producing assistant's findings for the
	 * dispatch target.
	 *
	 * @param context
	 *            the run context.
	 * @param result
	 *            the assistant result to apply.
	 * @param cleanupPolicy
	 *            the producing assistant's stale-finding cleanup policy.
	 * @param dispatchTarget
	 *            the entity the run was dispatched for; used so stale findings can
	 *            be reconciled even when this run produced no actions for it.
	 */
	AppliedAssistantResult apply(AssistantContext context, AssistantResult result,
			CleanupPolicy cleanupPolicy, EntityRef dispatchTarget);
}
