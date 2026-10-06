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
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Triggers database initialization (seed users, permissions, the dictionary, etc.). Replaces the
 * old {@code DatabaseInitializationListener}, which relied on {@code @WebListener} and required
 * {@code @ServletComponentScan}.
 * <p>
 * Issue #379: this used to run on {@code ApplicationReadyEvent}, after Tomcat was listening, so
 * on a fresh database every login failed for the minutes the dictionary took to load. It now
 * runs as a lifecycle phase that starts before the web server's ({@value #WEB_SERVER_PHASE}), so
 * the port opens only once initialization is complete.
 */
@Component
public class DatabaseInitializationRunner implements SmartLifecycle {

    /** {@code WebServerStartStopLifecycle}'s phase in Spring Boot 3.5. */
    static final int WEB_SERVER_PHASE = SmartLifecycle.DEFAULT_PHASE - 2048;

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializationRunner.class);

    private final DatabaseInitializer databaseInitializer;
    private volatile boolean running;

    public DatabaseInitializationRunner(DatabaseInitializer databaseInitializer) {
        this.databaseInitializer = databaseInitializer;
    }

    @Override
    public void start() {
        log.info("Running database initializers...");
        databaseInitializer.initialize();
        running = true;
        log.info("Database initialization complete.");
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /** Before the web server, so requests arrive only after initialization. */
    @Override
    public int getPhase() {
        return WEB_SERVER_PHASE - 2048;
    }
}
