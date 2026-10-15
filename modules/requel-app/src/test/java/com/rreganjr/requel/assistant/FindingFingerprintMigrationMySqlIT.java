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
package com.rreganjr.requel.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.rreganjr.requel.project.TargetFingerprint;

/**
 * The V25 migration (issue #270) on a database that already has findings: migrate to V24, write
 * a finding row, then migrate to V25. The existing finding has no fingerprint (read as not stale
 * until its next run), and the column holds a SHA-256 hex fingerprint. The
 * {@code LexicalAdvisoryMigrationMySqlIT} pattern: plain Flyway and JDBC, skipped (not failed)
 * without Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
public class FindingFingerprintMigrationMySqlIT {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@Test
	void v25AddsANullableFingerprintToExistingFindings() throws Exception {
		migrateTo("24");
		try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
				"INSERT INTO assistant_findings (id, idempotency_key, assistant_id, target_type,"
						+ " target_id, finding_type, state, created_run_id, last_seen_run_id)"
						+ " VALUES ('f-1', 'legacy-lexical:Goal:1:spelling', 'legacy-lexical',"
						+ " 'Goal', 1, 'spelling', 'ACTIVE', 'r-1', 'r-1')")) {
			ps.executeUpdate();
		}

		migrateTo("25");

		String fingerprint = TargetFingerprint.of("Login", "Users log in.");
		try (Connection c = connect()) {
			assertNull(fingerprint(c), "an existing finding has no fingerprint");
			try (PreparedStatement ps = c.prepareStatement(
					"UPDATE assistant_findings SET target_fingerprint = ? WHERE id = 'f-1'")) {
				ps.setString(1, fingerprint);
				ps.executeUpdate();
			}
			assertEquals(fingerprint, fingerprint(c));
		}
	}

	private static String fingerprint(Connection c) throws Exception {
		try (PreparedStatement ps = c.prepareStatement(
				"SELECT target_fingerprint FROM assistant_findings WHERE id = 'f-1'");
				ResultSet rs = ps.executeQuery()) {
			rs.next();
			return rs.getString(1);
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
}
