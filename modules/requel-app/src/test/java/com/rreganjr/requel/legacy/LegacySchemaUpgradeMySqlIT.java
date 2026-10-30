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
package com.rreganjr.requel.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.rreganjr.requel.RequelFlywayMigrationStrategy;
import com.rreganjr.requel.UnversionedDatabaseException;

/**
 * Issue #380: a Requel 1.0.1, 1.0.2 or 1.1.0 database (the fixtures in {@code upgrade/}, made from
 * the published images by scripts/legacy-fixtures.sh) upgrades in place to exactly the schema a
 * fresh install has; anything else is refused before it is touched. No Spring context: the
 * strategy and Flyway are driven directly (LegacyUpgradeAppMySqlIT starts the app on one).
 */
@Testcontainers(disabledWithoutDocker = true)
public class LegacySchemaUpgradeMySqlIT {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci");

	@ParameterizedTest
	@ValueSource(strings = { "1.0.1", "1.0.2", "1.1.0" })
	void anOldDatabaseEndsUpWithTheSchemaOfAFreshInstall(String version) throws Exception {
		String db = load(version, "up_" + version.replace('.', '_'));
		int users = count(db, "users");
		int goals = count(db, "goals");

		new RequelFlywayMigrationStrategy(true).migrate(flyway(db));

		String fresh = createDatabase("fresh_" + version.replace('.', '_'));
		new RequelFlywayMigrationStrategy().migrate(flyway(fresh));
		assertEquals(Optional.empty(), shape(db).firstDifference(shape(fresh)));
		assertEquals(flyway(fresh).info().current().getVersion(), flyway(db).info().current().getVersion());
		assertEquals("1", flyway(db).info().applied()[0].getVersion().getVersion(), "baselined at 1");
		assertEquals(users, count(db, "users"));
		assertEquals(goals, count(db, "goals"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "1.0.1", "1.0.2", "1.1.0" })
	void whatTheOldReleaseLeftOutIsFilledIn(String version) throws Exception {
		String db = load(version, "data_" + version.replace('.', '_'));
		assertEquals(0, count(db, "pods_scenarios"), "the old release never wrote it");
		assertTrue(count(db, "arguments WHERE support_level IS NULL") > 0, "the sample's argument has no level");

		new RequelFlywayMigrationStrategy(true).migrate(flyway(db));

		// WordNet's category 0 is still 0 after its column became AUTO_INCREMENT.
		assertEquals(1, count(db, "categorydef WHERE categoryid = 0"));
		// Every scenario is on its project's list, the way 2.x keeps it.
		assertEquals(count(db, "scenarios WHERE type LIKE '%Scenario'"), count(db, "pods_scenarios"));
		assertTrue(count(db, "pods_scenarios") > 0, "the fixture has a scenario");
		// An argument without a support level is Neutral (ordinal 2), as 2.x's import makes it.
		assertEquals(0, count(db, "arguments WHERE support_level IS NULL"));
		assertTrue(count(db, "arguments WHERE support_level = 2") > 0);
	}

	@Test
	void withoutThePropertyAnOldDatabaseIsRefusedAndLeftAlone() throws Exception {
		String db = load("1.0.2", "no_opt_in");
		LegacySchemaShape before = shape(db);

		UnversionedDatabaseException refused = assertThrows(UnversionedDatabaseException.class,
				() -> new RequelFlywayMigrationStrategy().migrate(flyway(db)));

		assertTrue(refused.getMessage().contains("requel.upgrade.from-1x"), refused.getMessage());
		assertEquals(Optional.empty(), shape(db).firstDifference(before));
	}

	@Test
	void aSchemaThatIsNotARecordedReleaseIsRefusedBeforeAnyChange() throws Exception {
		String db = load("1.0.2", "drifted");
		exec(db, "ALTER TABLE goals ADD COLUMN local_notes varchar(255)");
		LegacySchemaShape before = shape(db);

		LegacyUpgradeException refused = assertThrows(LegacyUpgradeException.class,
				() -> new RequelFlywayMigrationStrategy(true).migrate(flyway(db)));

		assertFalse(refused.isDatabaseChanged());
		assertTrue(refused.getMessage().contains("Closest is 1.0.2"), refused.getMessage());
		assertTrue(refused.getMessage().contains("local_notes"), refused.getMessage());
		assertEquals(Optional.empty(), shape(db).firstDifference(before));
		assertEquals(0, count(db, "information_schema.tables WHERE table_schema = '" + db
				+ "' AND table_name = 'flyway_schema_history'"));
	}

	@Test
	void anUnrelatedDatabaseIsRefused() throws Exception {
		String db = createDatabase("unrelated");
		exec(db, "CREATE TABLE notes (id BIGINT NOT NULL PRIMARY KEY)");

		LegacyUpgradeException refused = assertThrows(LegacyUpgradeException.class,
				() -> new RequelFlywayMigrationStrategy(true).migrate(flyway(db)));

		assertFalse(refused.isDatabaseChanged());
		assertEquals(0, count(db, "notes"), "still there, untouched");
	}

	@Test
	void theFailureAnalyzerSaysWhetherTheDatabaseWasChanged() {
		var untouched = new LegacyUpgradeFailureAnalyzer()
				.analyze(new RuntimeException(new LegacyUpgradeException("not a Requel schema", false)));
		assertTrue(untouched.getAction().startsWith("Nothing was changed."), untouched.getAction());
		var changed = new LegacyUpgradeFailureAnalyzer()
				.analyze(new RuntimeException(new LegacyUpgradeException("half done", true)));
		assertTrue(changed.getAction().startsWith("Restore the database"), changed.getAction());
	}

	// ---- helpers ------------------------------------------------------------------------------

	/** Loads upgrade/requel-<version>.sql into a new database with the mysql client. */
	static String load(String version, String db) throws Exception {
		createDatabase(db);
		String file = "/tmp/requel-" + version + ".sql";
		MYSQL.copyFileToContainer(MountableFile.forClasspathResource("upgrade/requel-" + version + ".sql"), file);
		var result = MYSQL.execInContainer("sh", "-c",
				"mysql -uroot -p" + MYSQL.getPassword() + " " + db + " < " + file);
		assertEquals(0, result.getExitCode(), result.getStderr());
		return db;
	}

	private static LegacySchemaShape shape(String db) throws Exception {
		try (Connection c = connect(db)) {
			return LegacySchemaShape.read(c, true);
		}
	}

	private static int count(String db, String from) throws Exception {
		try (Connection c = connect(db); Statement s = c.createStatement();
				ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + from)) {
			rs.next();
			return rs.getInt(1);
		}
	}

	private static void exec(String db, String sql) throws Exception {
		try (Connection c = connect(db); Statement s = c.createStatement()) {
			s.execute(sql);
		}
	}

	private static Flyway flyway(String db) {
		return Flyway.configure()
				.dataSource(url(db), "root", MYSQL.getPassword())
				.locations("classpath:db/migration")
				.load();
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
