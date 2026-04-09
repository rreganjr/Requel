/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr;

import org.springframework.core.task.TaskExecutor;

/**
 * Test-profile task executor that discards all submitted tasks.
 *
 * NLP analysis tasks submitted via {@code AssistantFacade} are fire-and-forget
 * in production, but running them synchronously in tests (via
 * {@code SyncTaskExecutor}) causes {@code LazyInitializationException} because
 * the Hibernate session has already closed when the analysis runs. Discarding
 * the tasks entirely is simpler and more correct for command integration tests
 * that are not testing the NLP pipeline.
 */
public class TestNoOpTaskExecutor implements TaskExecutor {

    @Override
    public void execute(Runnable task) {
        // Intentionally discard — NLP analysis is not under test here.
    }
}
