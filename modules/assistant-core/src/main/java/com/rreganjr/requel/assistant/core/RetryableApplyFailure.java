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

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Set;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import com.rreganjr.platform.exception.EntityLockException;

/**
 * Issue #495: whether an assistant run's apply phase failed in a way a fresh attempt can get past.
 * <p>
 * The apply phase writes findings through the command handler inside its own transaction, where
 * {@code RetryOnLockFailuresCommandHandler} deliberately doesn't retry (#247: an inner retry only
 * repeats doomed work in a rollback-only transaction). So the apply phase retries as a whole, for:
 * <ul>
 *   <li>a lost race on a shared row: a deadlock, a lock wait timeout, a stale or optimistic-lock
 *       version (positions are shared by text within a project, and two runs offering the same
 *       one collide on its version);</li>
 *   <li>a foreign-key violation: the target was deleted after the apply phase checked for it.
 *       The next attempt finds it gone and the run is skipped.</li>
 * </ul>
 * The whole cause chain is examined, so a failure wrapped by Spring, Hibernate or the run itself
 * still counts. Hibernate's own types are matched by name, so this module doesn't compile against
 * them.
 */
final class RetryableApplyFailure {

	/** Hibernate exceptions that mean a lost race or a vanished row, by class name. */
	private static final Set<String> HIBERNATE_RETRYABLE = Set.of(
			"org.hibernate.StaleStateException",
			"org.hibernate.StaleObjectStateException",
			"org.hibernate.PessimisticLockException",
			"org.hibernate.exception.LockAcquisitionException",
			"org.hibernate.exception.LockTimeoutException",
			"org.hibernate.exception.ConstraintViolationException",
			"org.hibernate.dialect.lock.OptimisticEntityLockException");

	/** SQLState for a deadlock or serialization failure. */
	private static final String SQLSTATE_SERIALIZATION_FAILURE = "40001";
	/** MySQL: deadlock found, lock wait timeout exceeded. */
	private static final int MYSQL_DEADLOCK = 1213;
	private static final int MYSQL_LOCK_WAIT_TIMEOUT = 1205;

	private RetryableApplyFailure() {
	}

	static boolean isRetryable(Throwable failure) {
		for (Throwable t = failure; t != null; t = t.getCause() == t ? null : t.getCause()) {
			if (t instanceof EntityLockException
					|| t instanceof ConcurrencyFailureException
					|| t instanceof DataIntegrityViolationException
					|| t instanceof jakarta.persistence.OptimisticLockException
					|| t instanceof jakarta.persistence.PessimisticLockException
					|| t instanceof jakarta.persistence.LockTimeoutException
					|| t instanceof SQLIntegrityConstraintViolationException
					|| isHibernateRetryable(t.getClass())) {
				return true;
			}
			if (t instanceof SQLException sql
					&& (SQLSTATE_SERIALIZATION_FAILURE.equals(sql.getSQLState())
							|| sql.getErrorCode() == MYSQL_DEADLOCK
							|| sql.getErrorCode() == MYSQL_LOCK_WAIT_TIMEOUT)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isHibernateRetryable(Class<?> type) {
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			if (HIBERNATE_RETRYABLE.contains(c.getName())) {
				return true;
			}
		}
		return false;
	}
}
