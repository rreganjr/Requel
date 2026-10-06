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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.exception.NoSuchUserException;

/**
 * Issue #379: the database initializers finish before the web server starts accepting
 * requests, so the first login on a fresh install works. Runs a real server on a random port and
 * looks for the built-in users at the moment it starts.
 */
@SpringBootTest(classes = { Application.class, DatabaseInitializationOrderIT.Probe.class },
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:db.properties", properties = {
		"db.name=requel_init_order_test",
		"db.driverUrl=jdbc:h2:mem:requel_init_order_test;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
})
@ActiveProfiles("test")
class DatabaseInitializationOrderIT {

	static final AtomicReference<Boolean> ADMIN_EXISTED_WHEN_SERVER_STARTED = new AtomicReference<>();

	@TestConfiguration
	static class Probe {
		@Bean
		ApplicationListener<WebServerInitializedEvent> recordUsersAtServerStart(UserRepository users) {
			return event -> {
				try {
					ADMIN_EXISTED_WHEN_SERVER_STARTED.set(users.findUserByUsername("admin") != null);
				} catch (NoSuchUserException e) {
					ADMIN_EXISTED_WHEN_SERVER_STARTED.set(false);
				}
			};
		}
	}

	@Test
	void theBuiltInUsersExistBeforeTheWebServerStarts() {
		assertThat(ADMIN_EXISTED_WHEN_SERVER_STARTED.get())
				.as("admin existed when the web server started")
				.isTrue();
	}
}
