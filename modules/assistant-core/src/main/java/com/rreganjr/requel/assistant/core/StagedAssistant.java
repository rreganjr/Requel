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

import java.util.List;

import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantResult;

/**
 * Issue #363: an assistant whose analysis waits on something outside Requel - a model call. The
 * worker runs {@link #prepare} inside its analyze transaction and {@link Stage#complete} after that
 * transaction has committed, so no database connection is held while the call runs.
 */
public interface StagedAssistant {

	/**
	 * Inside the analyze transaction: read everything the call needs - the target, its context, the
	 * project's settings. The stage holds only detached data, never an entity.
	 *
	 * @return the call, ready to make.
	 */
	Stage prepare(AssistantContext context, Object target) throws AssistantException;

	/** A prepared call. */
	@FunctionalInterface
	interface Stage {

		/**
		 * With no transaction open: make the call and build the results, one per member for a
		 * composed pass. Touches no entity and reads no store; usage and redaction rows are written
		 * in their own short transactions.
		 */
		List<AssistantResult> complete() throws AssistantException;
	}
}
