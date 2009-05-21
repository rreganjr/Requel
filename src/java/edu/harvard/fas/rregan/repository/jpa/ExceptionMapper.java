/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.repository.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.persistence.EntityExistsException;
import javax.persistence.OptimisticLockException;

import org.apache.log4j.Logger;
import org.hibernate.PropertyValueException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.validator.InvalidStateException;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.repository.EntityExceptionAdapter;

/**
 * Map exceptions related to persistence and validation activities or entities
 * from other packages to project specific exceptions for easier handling.
 * 
 * @author ron
 */
@Component("exceptionMapper")
@Scope("singleton")
public class ExceptionMapper {
	protected static final Logger log = Logger.getLogger(ExceptionMapper.class);
	private final Map<ExceptionMapKey, EntityExceptionAdapter> exceptionMap = new HashMap<ExceptionMapKey, EntityExceptionAdapter>();

	/**
	 * 
	 */
	public ExceptionMapper() {
		// TODO: make this configurable through spring
		addExceptionAdapter(PropertyValueException.class,
				new GenericPropertyValueExceptionAdapter());
		addExceptionAdapter(InvalidStateException.class, new InvalidStateExceptionAdapter());
		addExceptionAdapter(ConstraintViolationException.class,
				new ConstraintViolationExceptionAdapter("unknown"));
		addExceptionAdapter(OptimisticLockException.class, new OptimisticLockExceptionAdapter());
		addExceptionAdapter(StaleObjectStateException.class, new OptimisticLockExceptionAdapter());
		addExceptionAdapter(LockAcquisitionException.class, new OptimisticLockExceptionAdapter());
		addExceptionAdapter(CannotAcquireLockException.class, new OptimisticLockExceptionAdapter());
		addExceptionAdapter(HibernateOptimisticLockingFailureException.class,
				new OptimisticLockExceptionAdapter());

		addExceptionAdapter(EntityExistsException.class, new EntityExistsExceptionAdapter());
	}

	/**
	 * Use to programatically add exception mapping
	 * 
	 * @param exceptionType
	 * @param adapter
	 * @param entityClasses
	 */
	public void addExceptionAdapter(Class<? extends Throwable> exceptionType,
			EntityExceptionAdapter adapter, Class<?>... entityClasses) {
		if (entityClasses.length > 0) {
			for (Class<?> entityClass : entityClasses) {
				exceptionMap.put(new ExceptionMapKey(exceptionType, entityClass), adapter);
			}
		} else {
			exceptionMap.put(new ExceptionMapKey(exceptionType, null), adapter);
		}
	}

	/**
	 * Convert the supplied exception to an EntityException, or if the supplied
	 * exception is already an EntityException, return it. This method should
	 * only be used if more information about the entity that the exception is
	 * concerning is not known.
	 * 
	 * @see #convertException(Exception, Class, Object,
	 *      EntityExceptionActionType)
	 * @param exception
	 * @return
	 */
	public RuntimeException convertException(Exception exception) {
		return convertException(exception, null, null, EntityExceptionActionType.Unknown);
	}

	/**
	 * Convert the supplied exception to an EntityException, or if the supplied
	 * exception is already an EntityException, return it.
	 * 
	 * @param exception
	 * @param entityType
	 * @param entity
	 * @param actionType
	 * @return
	 */
	public RuntimeException convertException(Exception exception, Class<?> entityType,
			Object entity, EntityExceptionActionType actionType) {
		if (exception instanceof EntityException) {
			return (EntityException) exception;
		}

		log.debug("converting exception: " + exception, exception);

		// unwind nested exceptions with most specific on top
		Stack<Throwable> causeStack = getCauseStack(exception);
		while (!causeStack.empty()) {
			Throwable thrown = causeStack.pop();

			// look for a matching type up the hierarchy and through the
			// interfaces
			Class<?> currentEntityType = entityType;
			if (currentEntityType != null) {
				do {
					List<Class<?>> entityTypes = new ArrayList<Class<?>>();
					entityTypes.add(currentEntityType);
					Collections.addAll(entityTypes, currentEntityType.getInterfaces());
					for (Class<?> thisEntityType : entityTypes) {
						EntityExceptionAdapter adapter = exceptionMap.get(new ExceptionMapKey(
								thrown.getClass(), thisEntityType));
						if (adapter != null) {
							return adapter.convert(thrown, thisEntityType, entity, actionType);
						}
					}
					currentEntityType = currentEntityType.getSuperclass();
				} while (currentEntityType != null);
			} else {
				EntityExceptionAdapter adapter = exceptionMap.get(new ExceptionMapKey(thrown
						.getClass(), null));
				if (adapter != null) {
					return adapter.convert(thrown, null, entity, actionType);
				}
			}
		}

		// if the exception can't be converted, but is a runtime exception,
		// return it
		if (exception instanceof RuntimeException) {
			return (RuntimeException) exception;
		}
		return EntityException.forUnknownProblem(exception, entityType, entity, actionType);
	}

	private Stack<Throwable> getCauseStack(Throwable thrown) {
		Stack<Throwable> causeStack = new Stack<Throwable>();
		Throwable cause = thrown;
		do {
			causeStack.push(cause);
			cause = cause.getCause();
		} while (cause != null);
		return causeStack;
	}

	private static class ExceptionMapKey {
		private final Class<?> thrownType;
		private final Class<?> entityType;

		private ExceptionMapKey(Class<?> thrownType, Class<?> entityType) {
			this.thrownType = thrownType;
			this.entityType = entityType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
			result = prime * result + ((thrownType == null) ? 0 : thrownType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!getClass().isAssignableFrom(obj.getClass())) {
				return false;
			}
			final ExceptionMapKey other = (ExceptionMapKey) obj;
			if (entityType == null) {
				if (other.entityType != null) {
					return false;
				}
			} else if (!entityType.equals(other.entityType)) {
				return false;
			}
			if (thrownType == null) {
				if (other.thrownType != null) {
					return false;
				}
			} else if (!thrownType.equals(other.thrownType)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + ": thrown type = " + thrownType + " entity type = "
					+ entityType;
		}
	}
}
