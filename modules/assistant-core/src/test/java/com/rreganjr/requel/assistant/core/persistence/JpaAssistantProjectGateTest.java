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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.AssistantProjectGate.State;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.Query;

/**
 * #279. The gate is one statement that both locks the project row and reports whether it is
 * still there, so there are exactly three outcomes to pin down: a row came back, no row came
 * back, or the lock could not be taken.
 * <p>
 * Unit-level with a mocked {@link EntityManager} rather than only through
 * {@code DeleteProjectIT}: jacoco reports per module and the build has no
 * {@code report-aggregate}, so coverage earned by an integration test living in
 * {@code requel-app} is never attributed back to {@code assistant-core}. That is the lesson
 * from #253, where the same gap showed up as a 36% patch report on code the ITs did exercise.
 */
class JpaAssistantProjectGateTest {

	private static final EntityRef PROJECT = EntityRef.of("Project", 42L);

	private EntityManager entityManager;
	private Query query;
	private JpaAssistantProjectGate gate;

	@BeforeEach
	void setUp() {
		entityManager = mock(EntityManager.class);
		query = mock(Query.class);
		when(entityManager.createNativeQuery(anyString())).thenReturn(query);
		when(query.setParameter(1, 42L)).thenReturn(query);
		gate = new JpaAssistantProjectGate();
		ReflectionTestUtils.setField(gate, "entityManager", entityManager);
	}

	@Test
	void openWhenTheProjectRowIsStillThere() {
		when(query.getResultList()).thenReturn(List.of(42L));

		assertThat(gate.acquire(PROJECT)).isEqualTo(State.OPEN);
	}

	/**
	 * The whole point of the design: the statement that takes the lock is the statement that
	 * answers the question, so no window exists between checking and locking.
	 */
	@Test
	void locksForUpdateAndBindsTheProjectId() {
		when(query.getResultList()).thenReturn(List.of(42L));

		gate.acquire(PROJECT);

		verify(entityManager).createNativeQuery("SELECT id FROM pods WHERE id = ? FOR UPDATE");
		verify(query).setParameter(1, 42L);
	}

	@Test
	void goneWhenTheProjectRowHasBeenDeleted() {
		when(query.getResultList()).thenReturn(List.of());

		assertThat(gate.acquire(PROJECT)).isEqualTo(State.GONE);
	}

	@Test
	void busyWhenHibernateReportsAPessimisticLockFailure() {
		when(query.getResultList()).thenThrow(new PessimisticLockException("held"));

		assertThat(gate.acquire(PROJECT)).isEqualTo(State.BUSY);
	}

	@Test
	void busyWhenTheLockWaitTimesOut() {
		when(query.getResultList()).thenThrow(new LockTimeoutException("waited"));

		assertThat(gate.acquire(PROJECT)).isEqualTo(State.BUSY);
	}

	/** Spring's exception translation, where it is applied, rewraps the JPA pair as this. */
	@Test
	void busyWhenSpringTranslatesTheLockFailure() {
		when(query.getResultList())
				.thenThrow(new PessimisticLockingFailureException("translated"));

		assertThat(gate.acquire(PROJECT)).isEqualTo(State.BUSY);
	}
}
