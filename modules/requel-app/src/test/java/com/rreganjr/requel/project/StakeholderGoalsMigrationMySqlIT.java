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
package com.rreganjr.requel.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The V33 key change (issue #261): migrate to V32, where {@code stakeholders_goals.goals_id} is
 * unique and a second stakeholder cannot hold the same goal, write a pre-#261 row, then migrate to
 * the latest version. The row survives, two stakeholders can now hold one goal, and the same
 * stakeholder still cannot hold it twice.
 * <p>
 * Plain Flyway and JDBC, no Spring context: H2's {@code create-drop} schema never shows V1's key.
 * Skipped (not failed) when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
public class StakeholderGoalsMigrationMySqlIT {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@Test
	void v33LetsTwoStakeholdersHoldOneGoal() throws Exception {
		migrateTo("32");
		try (Connection c = connect()) {
			disableForeignKeys(c);
			hold(c, 1, 100);
			// The reason for V33: the V1 key refuses a second holder.
			assertThrows(SQLException.class, () -> hold(c, 2, 100));
		}

		migrateTo(null);

		try (Connection c = connect()) {
			disableForeignKeys(c);
			hold(c, 2, 100);
			assertEquals(2, holders(c, 100));
			assertThrows(SQLException.class, () -> hold(c, 2, 100), "still one row per pair");
		}
	}

	private static void hold(Connection c, long stakeholderId, long goalId) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(
				"INSERT INTO stakeholders_goals (abstract_stakeholder_id, goals_id) VALUES (?, ?)")) {
			ps.setLong(1, stakeholderId);
			ps.setLong(2, goalId);
			ps.executeUpdate();
		}
	}

	private static int holders(Connection c, long goalId) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(
				"SELECT COUNT(*) FROM stakeholders_goals WHERE goals_id = ?")) {
			ps.setLong(1, goalId);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getInt(1);
			}
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

	/** The rows reference stakeholders and goals that are not the point here. */
	private static void disableForeignKeys(Connection c) throws SQLException {
		try (Statement s = c.createStatement()) {
			s.execute("SET FOREIGN_KEY_CHECKS = 0");
		}
	}
}
