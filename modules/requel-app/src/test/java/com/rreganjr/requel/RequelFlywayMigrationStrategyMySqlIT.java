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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Issue #379: {@link RequelFlywayMigrationStrategy} against a real MySQL 8.4. Each case gets its
 * own database. Plain Flyway and JDBC, skipped (not failed) without Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
public class RequelFlywayMigrationStrategyMySqlIT {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@Test
	void anEmptyDatabaseIsMigratedToTheLatestVersion() throws Exception {
		String db = createDatabase("fresh");

		new RequelFlywayMigrationStrategy().migrate(flyway(db, null));

		assertEquals(latestVersion(), currentVersion(db));
	}

	@Test
	void aDatabaseWithHistoryIsMigratedTheRestOfTheWay() throws Exception {
		String db = createDatabase("from_v2");
		flyway(db, "2").migrate();
		assertEquals("2", currentVersion(db));

		new RequelFlywayMigrationStrategy().migrate(flyway(db, null));

		assertEquals(latestVersion(), currentVersion(db));
	}

	@Test
	void aDatabaseWithTablesButNoHistoryIsRefusedAndLeftAlone() throws Exception {
		String db = createDatabase("legacy");
		try (Connection c = connect(db); Statement s = c.createStatement()) {
			// The shape of a Requel 1.0.x table, made by Hibernate ddl-auto: no Flyway history.
			s.execute("CREATE TABLE goals (id BIGINT NOT NULL PRIMARY KEY, name VARCHAR(255))");
			s.execute("INSERT INTO goals VALUES (1, 'Keep my data')");
		}

		UnversionedDatabaseException refused = assertThrows(UnversionedDatabaseException.class,
				() -> new RequelFlywayMigrationStrategy().migrate(flyway(db, null)));

		assertEquals(db, refused.getDatabase());
		assertEquals(1, refused.getTableCount());
		assertTrue(refused.getMessage().contains("no Flyway history"), refused.getMessage());
		try (Connection c = connect(db); Statement s = c.createStatement()) {
			try (ResultSet rs = s.executeQuery("SELECT name FROM goals WHERE id = 1")) {
				assertTrue(rs.next());
				assertEquals("Keep my data", rs.getString(1));
			}
			try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM information_schema.tables"
					+ " WHERE table_schema = '" + db + "'")) {
				rs.next();
				assertEquals(1, rs.getInt(1), "nothing created, nothing dropped");
			}
		}
	}

	@Test
	void theFailureAnalyzerSaysWhatToDo() {
		var analysis = new RequelStartupFailureAnalyzer()
				.analyze(new RuntimeException(new UnversionedDatabaseException("requel", 81)));

		assertTrue(analysis.getDescription().contains("`requel` has 81 tables"));
		assertTrue(analysis.getAction().contains("export each project to XML"));
		assertFalse(analysis.getAction().isBlank());
	}

	private static Flyway flyway(String db, String target) {
		var config = Flyway.configure()
				.dataSource(url(db), "root", MYSQL.getPassword())
				.locations("classpath:db/migration");
		if (target != null) {
			config.target(target);
		}
		return config.load();
	}

	private static String latestVersion() throws Exception {
		String db = createDatabase("latest_probe");
		Flyway flyway = flyway(db, null);
		return flyway.info().all()[flyway.info().all().length - 1].getVersion().getVersion();
	}

	private static String currentVersion(String db) {
		var current = flyway(db, null).info().current();
		return current == null ? null : current.getVersion().getVersion();
	}

	private static String createDatabase(String name) throws Exception {
		try (Connection c = DriverManager.getConnection(url("requel"), "root", MYSQL.getPassword());
				Statement s = c.createStatement()) {
			s.execute("CREATE DATABASE IF NOT EXISTS `" + name + "`");
		}
		return name;
	}

	private static Connection connect(String db) throws Exception {
		return DriverManager.getConnection(url(db), "root", MYSQL.getPassword());
	}

	private static String url(String db) {
		return "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306) + "/" + db
				+ "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
	}
}
