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
package com.rreganjr.requel.assistant.core.persistence;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.AssistantProjectGate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.Query;

/**
 * Production {@link AssistantProjectGate}: one statement that both takes the project
 * row's write lock and answers whether the row still exists (issue #279).
 *
 * <p>
 * Deliberately native SQL rather than {@code entityManager.find(ProjectImpl.class, ...)}.
 * {@code assistant-core} depends on {@code project-domain} (interfaces) and
 * {@code annotation-jpa}, not on {@code project-jpa}, so the project's entity class is not
 * on this module's compile classpath; adding that dependency to reach one row would invert
 * the module graph. The table name is stable - see {@code AbstractProjectOrDomain}'s
 * {@code @Table(name = "pods")} - and {@code DeleteProjectMySqlIT} fails loudly if it ever
 * changes.
 * </p>
 *
 * <p>
 * {@code FOR UPDATE} (exclusive) rather than a shared read lock: H2 has no {@code FOR
 * SHARE}, so a shared-lock design would be untestable on the H2 profile, and Hibernate maps
 * {@code PESSIMISTIC_READ} to plain {@code FOR UPDATE} on several dialects anyway. The cost
 * is that two applies for the same project serialize, which is cheap - since #247 all the
 * slow work happens in the analyze phase and an apply is a handful of inserts.
 * </p>
 *
 * <p>
 * No {@code NOWAIT} and no lock-timeout hint: MySQL 8 supports {@code NOWAIT} and H2 does
 * not, and waiting is what we want here (see {@link AssistantProjectGate}). The wait is
 * bounded by the database's own lock-wait timeout, and the waiter is a worker thread on the
 * assistant task executor, never a request thread.
 * </p>
 *
 * <p>
 * {@code MANDATORY} propagation is load-bearing, not decoration: a row lock lives only as
 * long as the transaction that took it. If this ran in its own transaction the lock would
 * be released at its commit - before the caller wrote a single finding - and the gate would
 * silently guarantee nothing. Requiring the caller's transaction makes that mistake a
 * startup-time failure instead of a race nobody can reproduce.
 * </p>
 */
@Component
public class JpaAssistantProjectGate implements AssistantProjectGate {

	private static final Logger log = LoggerFactory.getLogger(JpaAssistantProjectGate.class);

	/**
	 * Locks the row and reports its existence in one round trip: a result means the project
	 * is there and ours until this transaction ends; no result means it is gone.
	 */
	private static final String LOCK_PROJECT_SQL = "SELECT id FROM pods WHERE id = ? FOR UPDATE";

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public State acquire(EntityRef projectRef) {
		Objects.requireNonNull(projectRef, "projectRef");
		try {
			Query query = entityManager.createNativeQuery(LOCK_PROJECT_SQL);
			query.setParameter(1, projectRef.entityId());
			return query.getResultList().isEmpty() ? State.GONE : State.OPEN;
		} catch (PessimisticLockException | LockTimeoutException
				| PessimisticLockingFailureException e) {
			// Hibernate surfaces a lock failure as one of the JPA pair; Spring's exception
			// translation, where it is applied, rewraps it as the third. Treat all the same.
			log.warn("could not lock Project#{} before applying assistant findings: {}",
					projectRef.entityId(), e.toString());
			return State.BUSY;
		}
	}
}
