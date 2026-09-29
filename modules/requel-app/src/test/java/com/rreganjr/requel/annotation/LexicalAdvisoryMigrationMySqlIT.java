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
package com.rreganjr.requel.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The V24 migration (issue #268) on a database that already has issues: migrate to V23, write
 * annotation rows, then migrate to V24. Lexical issues become advisory
 * ({@code must_be_resolved = 0}); other issues keep theirs; {@code project_assistant_settings}
 * exists with its composite key. The {@code IssueSeverityMigrationMySqlIT} pattern: plain Flyway
 * and JDBC, skipped (not failed) without Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
public class LexicalAdvisoryMigrationMySqlIT {

	private static final String ISSUE = "com.rreganjr.requel.annotation.Issue";
	private static final String LEXICAL_ISSUE = "com.rreganjr.requel.annotation.impl.LexicalIssue";

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@Test
	void v24MakesLexicalIssuesAdvisoryAndCreatesTheSettingsTable() throws Exception {
		migrateTo("23");

		long issueId;
		long lexicalId;
		try (Connection c = connect()) {
			try (Statement s = c.createStatement()) {
				// The rows reference a project and a user that are not the point here.
				s.execute("SET FOREIGN_KEY_CHECKS = 0");
			}
			issueId = insertAnnotation(c, ISSUE, "an open issue");
			lexicalId = insertAnnotation(c, LEXICAL_ISSUE, "'zorblat' may be misspelled");
		}

		migrateTo("24");

		try (Connection c = connect()) {
			assertEquals(1, mustBeResolved(c, issueId));
			assertEquals(0, mustBeResolved(c, lexicalId));
			try (Statement s = c.createStatement()) {
				s.executeUpdate("INSERT INTO project_assistant_settings (project_id, assistant_id,"
						+ " enabled) VALUES (1, 'legacy-lexical', 0)");
				boolean duplicateRefused = false;
				try {
					s.executeUpdate("INSERT INTO project_assistant_settings (project_id,"
							+ " assistant_id, enabled) VALUES (1, 'legacy-lexical', 1)");
				} catch (java.sql.SQLException expected) {
					duplicateRefused = true;
				}
				assertTrue(duplicateRefused, "one row per project and assistant");
			}
		}
	}

	private static void migrateTo(String version) {
		Flyway.configure()
				.dataSource(url(), MYSQL.getUsername(), MYSQL.getPassword())
				.locations("classpath:db/migration")
				.target(version)
				.load()
				.migrate();
	}

	private static Connection connect() throws Exception {
		return DriverManager.getConnection(url(), MYSQL.getUsername(), MYSQL.getPassword());
	}

	private static String url() {
		return MYSQL.getJdbcUrl() + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
	}

	private static long insertAnnotation(Connection c, String type, String text) throws Exception {
		try (PreparedStatement ps = c.prepareStatement(
				"INSERT INTO annotations (annotation_type, grouping_object_type, grouping_object_id,"
						+ " text, version, must_be_resolved, created_by_id)"
						+ " VALUES (?, 'com.rreganjr.requel.project.impl.ProjectImpl', 1, ?, 0, 1, 1)",
				Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, type);
			ps.setString(2, text);
			ps.executeUpdate();
			try (ResultSet keys = ps.getGeneratedKeys()) {
				keys.next();
				return keys.getLong(1);
			}
		}
	}

	private static int mustBeResolved(Connection c, long id) throws Exception {
		try (PreparedStatement ps = c.prepareStatement(
				"SELECT must_be_resolved FROM annotations WHERE id = ?")) {
			ps.setLong(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getInt(1);
			}
		}
	}
}
