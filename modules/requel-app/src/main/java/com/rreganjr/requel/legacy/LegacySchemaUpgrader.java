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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Issue #380: upgrades a Requel 1.0.1, 1.0.2 or 1.1.0 database in place, so Flyway can take it
 * from there.
 *
 * <ol>
 * <li>Read the schema's shape and match it exactly to one of the {@link LegacyRelease}s. Anything
 * else is refused before a single change: MySQL can't roll back DDL, so this check is what keeps
 * an unexpected schema from ending up half converted.</li>
 * <li>Convert the tables to V1's character set, then run that release's scripts on the same
 * connection. They turn the schema into V1's and repair the data the old release left
 * incomplete.</li>
 * <li>Check the result is exactly V1's shape, then baseline Flyway at 1. The caller then migrates,
 * applying V2 onward as on any other database.</li>
 * </ol>
 */
public class LegacySchemaUpgrader {

	private static final Logger log = LoggerFactory.getLogger(LegacySchemaUpgrader.class);

	static final String V1_COLLATION = "utf8mb4_0900_ai_ci";

	private final DataSource dataSource;

	public LegacySchemaUpgrader(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * @return the release the database was upgraded from
	 * @throws LegacyUpgradeException when it isn't a recorded release (nothing changed), or the
	 *         scripts didn't produce V1's schema (changed: restore the backup)
	 */
	public LegacyRelease upgrade(Flyway flyway) {
		try (Connection connection = dataSource.getConnection()) {
			String database = connection.getCatalog();
			LegacyRelease release = identify(database, LegacySchemaShape.read(connection));
			log.warn("Upgrading database `{}` in place from Requel {} (#380).", database, release.version());
			convertCharacterSet(connection);
			runScripts(connection, release);
			Optional<String> difference = LegacySchemaShape.read(connection, true)
					.firstDifference(LegacySchemaShape.load(LegacyRelease.V1_SHAPE));
			if (difference.isPresent()) {
				throw new LegacyUpgradeException("Upgrading database `" + database + "` from Requel "
						+ release.version() + " did not produce the expected schema (" + difference.get()
						+ "). The database has been partly changed; restore it from your backup.", true);
			}
			Flyway.configure()
					.configuration(flyway.getConfiguration())
					.baselineVersion("1")
					.baselineDescription("Requel " + release.version() + " upgraded in place (#380)")
					.load()
					.baseline();
			log.warn("Database `{}` now matches Requel's V1 schema; Flyway applies the rest.", database);
			return release;
		} catch (SQLException e) {
			throw new IllegalStateException("Could not upgrade the database in place", e);
		}
	}

	/**
	 * The scripts set FOREIGN_KEY_CHECKS and NO_AUTO_VALUE_ON_ZERO for their session; the
	 * connection comes from the application's pool, so both are put back afterwards.
	 */
	private static void runScripts(Connection connection, LegacyRelease release) throws SQLException {
		String sqlMode;
		try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery("SELECT @@SESSION.sql_mode")) {
			rs.next();
			sqlMode = rs.getString(1);
		}
		try {
			for (String script : release.scripts()) {
				ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource(script), "UTF-8"));
			}
		} finally {
			try (PreparedStatement ps = connection.prepareStatement("SET SESSION sql_mode = ?");
					Statement s = connection.createStatement()) {
				ps.setString(1, sqlMode);
				ps.execute();
				s.execute("SET FOREIGN_KEY_CHECKS = 1");
			}
		}
	}

	/**
	 * V1's tables are utf8mb4_0900_ai_ci. A 1.0.x database is latin1, so text a 2.x user types that
	 * latin1 can't hold would be refused; a 1.1 one has whatever the server defaulted to. Converts
	 * the database default and every table that differs. On a full install this rewrites the
	 * dictionary tables (about 1.8 million rows), so it can take several minutes.
	 */
	static void convertCharacterSet(Connection connection) throws SQLException {
		List<String> tables = new ArrayList<>();
		try (PreparedStatement ps = connection.prepareStatement("SELECT table_name FROM information_schema.tables"
				+ " WHERE table_schema = ? AND table_type = 'BASE TABLE' AND table_collation <> ?")) {
			ps.setString(1, connection.getCatalog());
			ps.setString(2, V1_COLLATION);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					tables.add(rs.getString(1));
				}
			}
		}
		try (Statement s = connection.createStatement()) {
			s.execute("ALTER DATABASE CHARACTER SET utf8mb4 COLLATE " + V1_COLLATION);
			for (String table : tables) {
				log.warn("Converting `{}` to utf8mb4.", table);
				s.execute("ALTER TABLE `" + table + "` CONVERT TO CHARACTER SET utf8mb4 COLLATE " + V1_COLLATION);
			}
		}
	}

	/** Matches the shape exactly to a recorded release, or refuses naming the closest one. */
	static LegacyRelease identify(String database, LegacySchemaShape shape) {
		LegacyRelease closest = null;
		int closestDistance = Integer.MAX_VALUE;
		for (LegacyRelease release : LegacyRelease.values()) {
			LegacySchemaShape recorded = LegacySchemaShape.load(release.shapeResource());
			if (shape.firstDifference(recorded).isEmpty()) {
				return release;
			}
			int distance = shape.distanceTo(recorded);
			if (distance < closestDistance) {
				closest = release;
				closestDistance = distance;
			}
		}
		LegacySchemaShape nearest = LegacySchemaShape.load(closest.shapeResource());
		throw new LegacyUpgradeException("Database `" + database + "` is not a Requel 1.0.1, 1.0.2 or 1.1.0"
				+ " schema, so it can't be upgraded in place. Closest is " + closest.version() + ", which differs in "
				+ closestDistance + " places, first " + shape.firstDifference(nearest).orElseThrow()
				+ ". Nothing was changed.", false);
	}
}
