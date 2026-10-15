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
package com.rreganjr.requel.service;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Every {@link IssueSeverityIT} case, run against a real MySQL 8.4 with the Flyway schema
 * (issue #271), so the V22 {@code annotations.severity} column, rather than Hibernate's
 * {@code create-drop} DDL, is what the mapping reads and writes.
 * <p>
 * Skipped (not failed) when Docker is not available on the build machine, so a green local build
 * without Docker is not evidence this path works.
 *
 * @author ron
 */
@Testcontainers(disabledWithoutDocker = true)
public class IssueSeverityMySqlIT extends IssueSeverityIT {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@DynamicPropertySource
	static void mysql(DynamicPropertyRegistry registry) {
		// IssueSeverityIT is @TestInstance(PER_CLASS): Spring builds the context while creating
		// the test instance, BEFORE the Testcontainers extension's beforeAll starts the @Container.
		// Start it here so the mapped port exists when the suppliers run (start() on a running
		// container is a no-op; the extension still stops it). Same reasoning as
		// DeleteProjectMySqlIT and EditPositionDedupMySqlIT.
		if (!MYSQL.isRunning()) {
			MYSQL.start();
		}
		registry.add("spring.datasource.url", () -> MYSQL.getJdbcUrl()
				+ "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC");
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
		registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
		// The production path: Flyway builds the schema (V1..Vn), Hibernate only maps it.
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.flyway.baseline-on-migrate", () -> "true");
		registry.add("spring.flyway.baseline-version", () -> "0");
		// Fail fast, visibly, if a lock is ever contended in this single-threaded test.
		registry.add("spring.datasource.hikari.connection-init-sql",
				() -> "SET SESSION innodb_lock_wait_timeout = 10");
	}
}
