/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.platform.exception;

/**
 * The caller's expected version no longer matches the entity: someone else saved it first. Issue
 * #345: a distinct type so {@code RetryOnLockFailuresCommandHandler} does not retry it (the
 * caller's version stays stale, so every retry fails the same way) and the API can report it as a
 * conflict (HTTP 409, gateway {@code CONFLICT}). Lock-acquisition failures and Hibernate's own
 * stale-state failures stay plain {@link EntityLockException}s and are still retried.
 */
public class StaleVersionException extends EntityLockException {
	static final long serialVersionUID = 0;

	protected StaleVersionException(Class<?> entityType, Object entity,
			EntityExceptionActionType actionType, String format, Object... messageArgs) {
		super(entityType, entity, null, null, actionType, format, messageArgs);
	}
}
