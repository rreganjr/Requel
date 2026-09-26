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
 * The V23 column change (issue #257) on a database that already has goal relations: migrate to
 * V22, where {@code relation_type} is still V1's {@code enum('Conflicts','Supports')} and rejects
 * any other value, write the relations a pre-#257 install holds, then migrate to the latest
 * version. The existing values survive, and every {@link GoalRelationType} can be stored.
 * <p>
 * Plain Flyway and JDBC, no Spring context: the point is the column, which H2's
 * {@code create-drop} schema never shows. Skipped (not failed) when Docker is not available.
 *
 * @author ron
 */
@Testcontainers(disabledWithoutDocker = true)
public class GoalRelationTypeMigrationMySqlIT {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@Test
	void v23KeepsExistingValuesAndAcceptsEveryRelationType() throws Exception {
		migrateTo("22");

		long supportsId;
		long conflictsId;
		try (Connection c = connect()) {
			disableForeignKeys(c);
			supportsId = insertRelation(c, "Supports", 1, 2);
			conflictsId = insertRelation(c, "Conflicts", 2, 1);
			// The reason for V23: the V1 enum column refuses the new values (strict mode).
			assertThrows(SQLException.class, () -> insertRelation(c, "Refines", 3, 4));
		}

		migrateTo(null);

		try (Connection c = connect()) {
			disableForeignKeys(c);
			assertEquals("Supports", relationTypeOf(c, supportsId));
			assertEquals("Conflicts", relationTypeOf(c, conflictsId));
			long from = 10;
			for (GoalRelationType type : GoalRelationType.values()) {
				long id = insertRelation(c, type.name(), from, from + 1);
				assertEquals(type.name(), relationTypeOf(c, id));
				from += 2;
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

	/** The rows reference goals and a user that are not the point here. */
	private static void disableForeignKeys(Connection c) throws SQLException {
		try (Statement s = c.createStatement()) {
			s.execute("SET FOREIGN_KEY_CHECKS = 0");
		}
	}

	private static long insertRelation(Connection c, String type, long fromGoalId, long toGoalId)
			throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(
				"INSERT INTO goal_relations (relation_type, version, created_by_id,"
						+ " from_goal_internal_id, to_goal_internal_id) VALUES (?, 0, 1, ?, ?)",
				Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, type);
			ps.setLong(2, fromGoalId);
			ps.setLong(3, toGoalId);
			ps.executeUpdate();
			try (ResultSet keys = ps.getGeneratedKeys()) {
				keys.next();
				return keys.getLong(1);
			}
		}
	}

	private static String relationTypeOf(Connection c, long id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(
				"SELECT relation_type FROM goal_relations WHERE id = ?")) {
			ps.setLong(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getString(1);
			}
		}
	}
}
