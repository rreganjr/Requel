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
 * A write refused because it would duplicate a value that must be unique (a name already taken,
 * text another position already has). Issue #345: a distinct type so the API can report it as a
 * conflict the caller can fix (HTTP 409, gateway {@code CONFLICT}) rather than a server failure.
 * Still an {@link EntityException}, so code catching that is unaffected.
 */
public class UniquenessConflictException extends EntityException {
	static final long serialVersionUID = 0;

	protected UniquenessConflictException(Class<?> entityType, Object entity,
			String[] entityPropertyNames, Object[] entityPropertyValues,
			EntityExceptionActionType actionType, String format, Object... messageArgs) {
		super(entityType, entity, entityPropertyNames, entityPropertyValues, actionType, format,
				messageArgs);
	}

	protected UniquenessConflictException(Throwable cause, Class<?> entityType, Object entity,
			String[] entityPropertyNames, Object[] entityPropertyValues,
			EntityExceptionActionType actionType, String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyNames, entityPropertyValues, actionType,
				format, messageArgs);
	}
}
