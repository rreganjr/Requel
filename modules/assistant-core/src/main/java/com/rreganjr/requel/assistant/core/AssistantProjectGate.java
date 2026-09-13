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

import com.rreganjr.requel.assistant.api.EntityRef;

/**
 * Serializes an assistant run's apply phase against a concurrent project delete
 * (issue #279).
 * <p>
 * A run is two transactions: a slow <em>analyze</em> that writes nothing, then a short
 * <em>apply</em> that writes the findings. Between them the user can delete the project.
 * {@code annotations.grouping_object_id} is an {@code @Any} soft reference with no
 * foreign key, so the database will happily accept findings filed under a project row
 * that no longer exists - unreachable orphans that no cascade will ever collect.
 * <p>
 * The gate closes that window by taking the project row's write lock before the apply
 * writes anything. {@code DeleteProjectCommandImpl} takes the <em>same</em> lock on the
 * <em>same</em> row before it touches any child, so both paths share one lock order and
 * cannot deadlock, and only two interleavings are possible:
 * <ul>
 * <li>the apply wins the lock: the delete waits, then its own grouping-object sweep
 * removes whatever the apply wrote;</li>
 * <li>the delete wins: the apply waits until the delete commits, then finds the row gone
 * and cancels itself without writing.</li>
 * </ul>
 * Waiting is therefore the intended behaviour, not a cost to tune away.
 */
public interface AssistantProjectGate {

	/** What {@link #acquire(EntityRef)} found. */
	enum State {
		/** The project row exists and its write lock is held for the rest of this transaction. */
		OPEN,
		/** The project row is gone; anything applied now would be an orphan. */
		GONE,
		/** The lock could not be taken before the database's lock-wait timeout fired. */
		BUSY
	}

	/**
	 * Lock and check the project row in one statement.
	 *
	 * @param projectRef
	 *            the run's project. Must not be {@code null}; callers skip the gate
	 *            entirely when {@code AnalysisRequest.projectRef()} is {@code null}.
	 * @return whether the apply may proceed.
	 */
	State acquire(EntityRef projectRef);
}
