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
package com.rreganjr.requel.user;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * #446: V41 adds {@code users.password_changed_at}, NULL for every existing user, so no existing
 * sign-in is refused by the upgrade.
 */
@Testcontainers(disabledWithoutDocker = true)
public class UsersPasswordChangedAtMigrationMySqlIT {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@Test
	void v41AddsAnEmptyPasswordChangedAtForExistingUsers() throws Exception {
		migrateTo("40");
		try (Connection c = connect()) {
			exec(c, "SET FOREIGN_KEY_CHECKS = 0");
			exec(c, "INSERT INTO users (id, editable, email_address, hashed_password, username, version, organization_id)"
					+ " VALUES (10, b'1', 'existing@example.invalid', 'x', 'existing', 0, 1)");
		}

		migrateTo(null);

		try (Connection c = connect();
				Statement s = c.createStatement();
				ResultSet rs = s.executeQuery("SELECT password_changed_at FROM users WHERE id = 10")) {
			assertTrue(rs.next());
			assertNull(rs.getTimestamp(1), "an existing user has no password change recorded");
		}
	}

	private static void exec(Connection c, String sql) throws SQLException {
		try (Statement s = c.createStatement()) {
			s.execute(sql);
		}
	}

	private static void migrateTo(String version) {
		var config = Flyway.configure()
				.dataSource(url(), MYSQL.getUsername(), MYSQL.getPassword())
				.locations("classpath:db/migration");
		if (version != null) {
			config = config.target(version);
		}
		config.load().migrate();
	}

	private static Connection connect() throws Exception {
		return DriverManager.getConnection(url(), MYSQL.getUsername(), MYSQL.getPassword());
	}

	private static String url() {
		return MYSQL.getJdbcUrl() + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
	}
}
