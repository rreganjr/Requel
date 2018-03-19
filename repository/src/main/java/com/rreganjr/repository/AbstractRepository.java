/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
package com.rreganjr.repository;

import com.rreganjr.EntityException;
import com.rreganjr.EntityExceptionActionType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author ron
 */
public abstract class AbstractRepository implements Repository {
	protected static final Log log = LogFactory.getLog(AbstractRepository.class);

	private final ExceptionMapper exceptionMapper;

	protected AbstractRepository(ExceptionMapper exceptionMapper) {
		this.exceptionMapper = exceptionMapper;
	}

	/**
	 * Proxies and lazy loading are not implemented in the simple repository so
	 * just return the supplied object.
	 */
	@Override
	public <T> T initialize(T entity) throws EntityException {
		return entity;
	}

	public void flush() throws EntityException {
		// nothing to do
	}

	protected ExceptionMapper getExceptionMapper() {
		return exceptionMapper;
	}

	protected void addExceptionAdapter(Class<? extends Throwable> exceptionType,
			EntityExceptionAdapter adapter, Class<?>... entityClasses) {
		getExceptionMapper().addExceptionAdapter(exceptionType, adapter, entityClasses);
	}

	public RuntimeException convertException(Exception exception) {
		return getExceptionMapper().convertException(exception);
	}

	public RuntimeException convertException(Exception exception, Class<?> entityType,
			Object entity, EntityExceptionActionType actionType) {
		return getExceptionMapper().convertException(exception, entityType, entity, actionType);
	}
}
