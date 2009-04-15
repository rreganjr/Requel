/*
 * $Id: AbstractRepository.java,v 1.2 2009/01/03 10:24:35 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.repository;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.repository.jpa.ExceptionMapper;

/**
 * @author ron
 */
public abstract class AbstractRepository implements Repository {
	protected static final Logger log = Logger.getLogger(AbstractRepository.class);

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
