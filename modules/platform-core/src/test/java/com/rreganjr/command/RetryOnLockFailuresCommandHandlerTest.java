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
package com.rreganjr.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.exception.LockAcquisitionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * #247: lock-failure retries happen only at the outermost command, never inside
 * a cascade sub-command that joined the caller's transaction.
 */
class RetryOnLockFailuresCommandHandlerTest {

	/** A command whose first {@code failuresBeforeSuccess} executions throw a lock failure. */
	private static final class FlakyCommand implements Command {
		private final AtomicInteger executions = new AtomicInteger();
		private final int failuresBeforeSuccess;

		FlakyCommand(int failuresBeforeSuccess) {
			this.failuresBeforeSuccess = failuresBeforeSuccess;
		}

		@Override
		public void execute() {
			if (executions.incrementAndGet() <= failuresBeforeSuccess) {
				throw new LockAcquisitionException("Deadlock found when trying to get lock",
						new java.sql.SQLException("Deadlock", "40001", 1213));
			}
		}
	}

	private static final CommandHandler DIRECT = new CommandHandler() {
		@Override
		public <T extends Command> T execute(T command) throws Exception {
			command.execute();
			return command;
		}
	};

	@AfterEach
	void clearTransactionState() {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			TransactionSynchronizationManager.setActualTransactionActive(false);
		}
	}

	@Test
	void outermostCommandIsRetriedAfterALockFailure() throws Exception {
		FlakyCommand command = new FlakyCommand(1);
		new RetryOnLockFailuresCommandHandler(DIRECT, null).execute(command);
		assertThat(command.executions.get()).isEqualTo(2);
	}

	@Test
	void outermostCommandGivesUpAfterThreeLockFailures() {
		FlakyCommand command = new FlakyCommand(10);
		assertThatThrownBy(() -> new RetryOnLockFailuresCommandHandler(DIRECT, null).execute(command))
				.isInstanceOf(LockAcquisitionException.class);
		assertThat(command.executions.get()).isEqualTo(3);
	}

	@Test
	void nestedCommandInsideAnActiveTransactionIsNotRetried() {
		TransactionSynchronizationManager.setActualTransactionActive(true);
		FlakyCommand command = new FlakyCommand(1);
		assertThatThrownBy(() -> new RetryOnLockFailuresCommandHandler(DIRECT, null).execute(command))
				.isInstanceOf(LockAcquisitionException.class);
		assertThat(command.executions.get()).isEqualTo(1);
	}
}
