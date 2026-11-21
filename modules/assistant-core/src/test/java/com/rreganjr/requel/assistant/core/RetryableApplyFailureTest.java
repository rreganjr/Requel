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

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.exception.EntityLockException;

/** Issue #495: which apply-phase failures a fresh attempt can get past. */
class RetryableApplyFailureTest {

	@Test
	void lostRacesAreRetryable() {
		assertThat(RetryableApplyFailure.isRetryable(EntityLockException.staleEntity(Object.class,
				null, EntityExceptionActionType.Updating))).isTrue();
		assertThat(RetryableApplyFailure.isRetryable(EntityLockException.deadlock(Object.class,
				null, EntityExceptionActionType.Updating))).isTrue();
		assertThat(RetryableApplyFailure.isRetryable(new CannotAcquireLockException("deadlock")))
				.isTrue();
		assertThat(RetryableApplyFailure.isRetryable(
				new ObjectOptimisticLockingFailureException(Object.class, 1L))).isTrue();
		assertThat(RetryableApplyFailure.isRetryable(
				new jakarta.persistence.OptimisticLockException("stale"))).isTrue();
		assertThat(RetryableApplyFailure.isRetryable(
				new jakarta.persistence.PessimisticLockException("locked"))).isTrue();
		assertThat(RetryableApplyFailure.isRetryable(
				new jakarta.persistence.LockTimeoutException("timeout"))).isTrue();
	}

	@Test
	void vanishedRowsAreRetryable() {
		assertThat(RetryableApplyFailure.isRetryable(new DataIntegrityViolationException("FK")))
				.isTrue();
		assertThat(RetryableApplyFailure.isRetryable(
				new SQLIntegrityConstraintViolationException("FK", "23000", 1452))).isTrue();
	}

	@Test
	void mysqlDeadlockAndLockWaitCodesAreRetryable() {
		assertThat(RetryableApplyFailure.isRetryable(new SQLException("deadlock", "40001", 1213)))
				.isTrue();
		assertThat(RetryableApplyFailure.isRetryable(new SQLException("deadlock", "HY000", 1213)))
				.isTrue();
		assertThat(RetryableApplyFailure.isRetryable(new SQLException("lock wait", "HY000", 1205)))
				.isTrue();
	}

	@Test
	void aWrappedRaceIsRetryable() {
		RuntimeException wrapped = new IllegalStateException("apply failed",
				new RuntimeException("flush failed", new SQLException("deadlock", "40001", 1213)));
		assertThat(RetryableApplyFailure.isRetryable(wrapped)).isTrue();
	}

	@Test
	void hibernateTypesAreMatchedByName() {
		assertThat(RetryableApplyFailure.isRetryable(new org.hibernate.StaleObjectStateException(
				"Goal", 1L))).isTrue();
		assertThat(RetryableApplyFailure.isRetryable(
				new org.hibernate.exception.LockAcquisitionException("deadlock",
						new SQLException("deadlock", "40001", 1213)))).isTrue();
	}

	@Test
	void otherFailuresAreNotRetryable() {
		assertThat(RetryableApplyFailure.isRetryable(new IllegalStateException("boom"))).isFalse();
		assertThat(RetryableApplyFailure.isRetryable(new DataRetrievalFailureException("gone")))
				.isFalse();
		assertThat(RetryableApplyFailure.isRetryable(new SQLException("syntax", "42000", 1064)))
				.isFalse();
		assertThat(RetryableApplyFailure.isRetryable(null)).isFalse();
	}
}
