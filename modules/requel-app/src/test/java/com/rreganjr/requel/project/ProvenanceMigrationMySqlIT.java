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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The V26 migration (issue #272) on a database that has #71 provenance notes: migrate to V25,
 * write goals with notes rendered by the real #71 {@code ProvenanceNotes} (kept as fixtures under
 * {@code provenance/}, since the renderer is deleted in #272), then migrate to V26. Each
 * well-formed note becomes a source and a DERIVED_FROM link and is deleted; a person's note and a
 * malformed block are left alone. The {@code LexicalAdvisoryMigrationMySqlIT} pattern: plain
 * Flyway and JDBC, skipped (not failed) without Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
public class ProvenanceMigrationMySqlIT {

	private static final String NOTE = "com.rreganjr.requel.annotation.Note";
	private static final long PROJECT = 7;

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@Test
	void v26ConvertsProvenanceNotesToSourcesAndLinks() throws Exception {
		migrateTo("25");

		long withRef;
		long withoutRef;
		long sharedTicket;
		long human;
		long malformed;
		long withRefNote;
		long humanNote;
		long malformedNote;
		try (Connection c = connect()) {
			try (Statement s = c.createStatement()) {
				// The rows reference a project and a user that are not the point here.
				s.execute("SET FOREIGN_KEY_CHECKS = 0");
			}
			withRef = insertGoal(c, "Admins can end a room");
			withRefNote = insertNote(c, withRef, fixture("v71-note-with-criterion-ref.txt"));
			withoutRef = insertGoal(c, "Recordings are archived");
			insertNote(c, withoutRef, fixture("v71-note-without-criterion-ref.txt"));
			// A second criterion of the same ticket: one source, two links.
			sharedTicket = insertGoal(c, "Alarms route to Conduit");
			insertNote(c, sharedTicket, fixture("v71-note-with-criterion-ref.txt")
					.replace("\"AC-3\"", "\"AC-4\""));
			human = insertGoal(c, "A goal with a person's note");
			humanNote = insertNote(c, human, "Check this with Chris before sign-off.");
			malformed = insertGoal(c, "A goal with a broken block");
			malformedNote = insertNote(c, malformed,
					"Preamble\n\n```requel-provenance\n{ not json\n```");
		}

		migrateTo("26");

		try (Connection c = connect()) {
			List<String[]> sources = rows(c, "SELECT source_system, external_id, locator_type, locator"
					+ " FROM external_sources WHERE project_id = " + PROJECT
					+ " ORDER BY external_id", 4);
			assertEquals(2, sources.size(), "one source per ticket");
			assertArrayEquals(new String[] { "jira", "CON-3685", "URL",
					"https://platformq.atlassian.net/browse/CON-3685" }, sources.get(0));
			assertArrayEquals(new String[] { "jira", "CON-3686", null, null }, sources.get(1));

			List<String[]> links = rows(c, "SELECT l.target_id, s.external_id, l.fragment,"
					+ " l.fragment_key, l.fragment_hash, l.entity_fingerprint, l.relation,"
					+ " l.target_type FROM entity_source_links l"
					+ " JOIN external_sources s ON s.id = l.source_id ORDER BY l.target_id", 8);
			assertEquals(3, links.size());
			assertArrayEquals(new String[] { "" + withRef, "CON-3685", "AC-3", "AC-3",
					"9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", null,
					"DERIVED_FROM", "Goal" }, links.get(0));
			assertArrayEquals(new String[] { "" + withoutRef, "CON-3686", "hash:60303ae22b99",
					"hash:60303ae22b99",
					"60303ae22b998861bce3b28f33eec1be758a213c86c93c076dbe9f558c11c752", null,
					"DERIVED_FROM", "Goal" }, links.get(1));
			assertEquals("AC-4", links.get(2)[2]);
			assertEquals("CON-3685", links.get(2)[1]);

			assertFalse(exists(c, "SELECT 1 FROM annotations WHERE id = " + withRefNote),
					"a converted note is deleted");
			assertFalse(exists(c, "SELECT 1 FROM goals_annotations WHERE goal_impl_id IN ("
					+ withRef + "," + withoutRef + "," + sharedTicket + ")"));
			assertTrue(exists(c, "SELECT 1 FROM annotations WHERE id = " + humanNote),
					"a person's note stays");
			assertTrue(exists(c, "SELECT 1 FROM goals_annotations WHERE annotations_id = "
					+ humanNote));
			assertTrue(exists(c, "SELECT 1 FROM annotations WHERE id = " + malformedNote),
					"a malformed block stays");
			assertFalse(exists(c, "SELECT 1 FROM entity_source_links WHERE target_id IN ("
					+ human + "," + malformed + ")"));
			assertFalse(exists(c, "SHOW TABLES LIKE 'v26_%'"), "working tables are dropped");

			// External ids and fragments compare exactly (binary collation).
			try (Statement s = c.createStatement()) {
				s.executeUpdate("INSERT INTO external_sources (project_id, source_system, external_id)"
						+ " VALUES (" + PROJECT + ", 'jira', 'con-3685')");
			}
			assertEquals(3, rows(c, "SELECT id FROM external_sources", 1).size(),
					"con-3685 is a different source from CON-3685");
		}
	}

	private static void assertArrayEquals(String[] expected, String[] actual) {
		assertEquals(java.util.Arrays.asList(expected), java.util.Arrays.asList(actual));
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

	private static String fixture(String name) throws Exception {
		try (InputStream in = ProvenanceMigrationMySqlIT.class.getResourceAsStream(
				"/provenance/" + name)) {
			assertTrue(in != null, "missing fixture " + name);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static long insertGoal(Connection c, String name) throws Exception {
		try (PreparedStatement ps = c.prepareStatement(
				"INSERT INTO goals (version, text, name, created_by_id, projectordomain_id)"
						+ " VALUES (0, ?, ?, 1, ?)",
				Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, name + ".");
			ps.setString(2, name);
			ps.setLong(3, PROJECT);
			ps.executeUpdate();
			return key(ps);
		}
	}

	private static long insertNote(Connection c, long goalId, String text) throws Exception {
		long noteId;
		try (PreparedStatement ps = c.prepareStatement(
				"INSERT INTO annotations (annotation_type, grouping_object_type, grouping_object_id,"
						+ " text, version, must_be_resolved, created_by_id, date_created)"
						+ " VALUES (?, 'Project', ?, ?, 0, 0, 1, NOW(6))",
				Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, NOTE);
			ps.setLong(2, PROJECT);
			ps.setString(3, text);
			ps.executeUpdate();
			noteId = key(ps);
		}
		try (Statement s = c.createStatement()) {
			s.executeUpdate("INSERT INTO goals_annotations (goal_impl_id, annotations_id) VALUES ("
					+ goalId + ", " + noteId + ")");
			s.executeUpdate("INSERT INTO annotation_annotatable (annotation_id, annotatable_type,"
					+ " annotatable_id) VALUES (" + noteId + ", 'Goal', " + goalId + ")");
		}
		return noteId;
	}

	private static long key(PreparedStatement ps) throws Exception {
		try (ResultSet keys = ps.getGeneratedKeys()) {
			keys.next();
			return keys.getLong(1);
		}
	}

	private static List<String[]> rows(Connection c, String sql, int columns) throws Exception {
		List<String[]> out = new ArrayList<>();
		try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
			while (rs.next()) {
				String[] row = new String[columns];
				for (int i = 0; i < columns; i++) {
					row[i] = rs.getString(i + 1);
				}
				out.add(row);
			}
		}
		return out;
	}

	private static boolean exists(Connection c, String sql) throws Exception {
		try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
			return rs.next();
		}
	}
}
