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
package com.rreganjr.requel;

import com.rreganjr.platform.bootstrap.DatabaseInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Triggers database initialization (seed users, permissions, etc.) after the
 * Spring context is fully started. Replaces the old {@code DatabaseInitializationListener}
 * which relied on {@code @WebListener} and required {@code @ServletComponentScan}.
 */
@Component
public class DatabaseInitializationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializationRunner.class);

    private final DatabaseInitializer databaseInitializer;

    public DatabaseInitializationRunner(DatabaseInitializer databaseInitializer) {
        this.databaseInitializer = databaseInitializer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Running database initializers...");
        databaseInitializer.initialize();
        log.info("Database initialization complete.");
    }
}
