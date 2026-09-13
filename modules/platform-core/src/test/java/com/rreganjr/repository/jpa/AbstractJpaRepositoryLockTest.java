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
package com.rreganjr.repository.jpa;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

/**
 * {@code lockForUpdate} (issue #279): the ordered gate both {@code DeleteProjectCommandImpl}
 * and the assistant apply phase take before touching anything else, so that a cascading
 * delete and a concurrent writer reaching the same rows by a different path cannot interleave
 * or deadlock.
 * <p>
 * The behaviour worth pinning down is the guard around the lock itself.
 * {@link AbstractJpaRepository#attach(EntityManager, Object)} returns the <em>detached
 * original</em> when the row is gone, and {@code EntityManager.lock} throws on a detached
 * instance - so locking has to be conditional on the entity actually being managed, and a
 * missing row must leave the caller to fail on its own terms rather than here.
 */
class AbstractJpaRepositoryLockTest {

	private EntityManager entityManager;
	private AbstractJpaRepository repository;

	/** Minimal entity: {@code attach} finds the id reflectively via {@code getId()}. */
	public static class Thing {
		private final Long id;

		public Thing(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@BeforeEach
	void setUp() {
		entityManager = mock(EntityManager.class);
		repository = new AbstractJpaRepository(new ExceptionMapper());
		ReflectionTestUtils.setField(repository, "entityManager", entityManager);
	}

	@Test
	void nullEntityIsANoOp() {
		assertNull(repository.lockForUpdate(null));
		verify(entityManager, never()).lock(any(), any());
	}

	@Test
	void takesAPessimisticWriteLockOnAManagedEntity() {
		Thing thing = new Thing(7L);
		when(entityManager.contains(thing)).thenReturn(true);

		assertSame(thing, repository.lockForUpdate(thing));

		verify(entityManager).lock(thing, LockModeType.PESSIMISTIC_WRITE);
	}

	/**
	 * The row is gone: {@code attach} hands back the detached original, and locking that
	 * would throw. Nothing to gate, so nothing is locked - the caller fails where it would
	 * have before this method existed.
	 */
	@Test
	void doesNotLockWhenTheRowNoLongerExists() {
		Thing thing = new Thing(7L);
		when(entityManager.contains(thing)).thenReturn(false);
		when(entityManager.find(Thing.class, 7L)).thenReturn(null);

		assertSame(thing, repository.lockForUpdate(thing));

		verify(entityManager, never()).lock(any(), any());
	}

	/**
	 * A lock failure reaches the caller unchanged, and that is deliberate rather than a gap.
	 * {@code ExceptionMapper} registers its default adapters under the key
	 * {@code (exceptionType, null)}, but {@code convertException} only consults that key when
	 * the entity type is null - and this method, like {@code get}, {@code merge} and
	 * {@code delete}, passes {@code entity.getClass()}. So an unmapped {@link RuntimeException}
	 * is returned as-is by the mapper and rethrown here. {@code lockForUpdate} behaves exactly
	 * like its siblings; a caller that wants a lock timeout surfaced differently has to map it.
	 */
	@Test
	void aLockFailurePropagatesThroughTheExceptionMapperUnchanged() {
		Thing thing = new Thing(7L);
		IllegalStateException boom = new IllegalStateException("lock wait timeout");
		when(entityManager.contains(thing)).thenReturn(true);
		org.mockito.Mockito.doThrow(boom).when(entityManager)
				.lock(thing, LockModeType.PESSIMISTIC_WRITE);

		assertSame(boom, assertThrows(IllegalStateException.class,
				() -> repository.lockForUpdate(thing)));
	}
}
