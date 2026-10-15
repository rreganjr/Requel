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
import static org.junit.jupiter.api.Assertions.assertNull;

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
 * The V22 backfill (issue #271) on a database that already has issues: migrate to V21, write
 * annotation rows the way a pre-#271 install holds them, then migrate to V22. Issues become
 * {@code MEDIUM}, lexical issues {@code LOW}, and notes, which share the single-table
 * {@code annotations} with issues, stay {@code NULL}.
 * <p>
 * Plain Flyway and JDBC, no Spring context: the point is the SQL, not the mapping
 * ({@code IssueSeverityMySqlIT} covers the mapping on the Flyway schema). Skipped (not failed)
 * when Docker is not available.
 *
 * @author ron
 */
@Testcontainers(disabledWithoutDocker = true)
public class IssueSeverityMigrationMySqlIT {

	private static final String ISSUE = "com.rreganjr.requel.annotation.Issue";
	private static final String LEXICAL_ISSUE = "com.rreganjr.requel.annotation.impl.LexicalIssue";
	private static final String NOTE = "com.rreganjr.requel.annotation.Note";

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@Test
	void v22BackfillsIssueRowsByKindAndLeavesNotesNull() throws Exception {
		migrateTo("21");

		long issueId;
		long lexicalId;
		long noteId;
		try (Connection c = connect()) {
			try (Statement s = c.createStatement()) {
				// The rows reference a project and a user that are not the point here.
				s.execute("SET FOREIGN_KEY_CHECKS = 0");
			}
			issueId = insertAnnotation(c, ISSUE, "an open issue", true);
			lexicalId = insertAnnotation(c, LEXICAL_ISSUE, "'zorblat' may be misspelled", true);
			noteId = insertAnnotation(c, NOTE, "a note", null);
		}

		migrateTo("22");

		try (Connection c = connect()) {
			assertEquals("MEDIUM", severityOf(c, issueId));
			assertEquals("LOW", severityOf(c, lexicalId));
			assertNull(severityOf(c, noteId));
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

	private static long insertAnnotation(Connection c, String type, String text,
			Boolean mustBeResolved) throws Exception {
		try (PreparedStatement ps = c.prepareStatement(
				"INSERT INTO annotations (annotation_type, grouping_object_type, grouping_object_id,"
						+ " text, version, must_be_resolved, created_by_id)"
						+ " VALUES (?, 'com.rreganjr.requel.project.impl.ProjectImpl', 1, ?, 0, ?, 1)",
				Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, type);
			ps.setString(2, text);
			if (mustBeResolved == null) {
				ps.setNull(3, java.sql.Types.BIT);
			} else {
				ps.setBoolean(3, mustBeResolved);
			}
			ps.executeUpdate();
			try (ResultSet keys = ps.getGeneratedKeys()) {
				keys.next();
				return keys.getLong(1);
			}
		}
	}

	private static String severityOf(Connection c, long id) throws Exception {
		try (PreparedStatement ps = c.prepareStatement(
				"SELECT severity FROM annotations WHERE id = ?")) {
			ps.setLong(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getString(1);
			}
		}
	}
}
