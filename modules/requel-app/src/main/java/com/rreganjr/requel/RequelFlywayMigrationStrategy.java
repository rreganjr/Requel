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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;

import com.rreganjr.requel.legacy.LegacySchemaUpgrader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.stereotype.Component;

/**
 * Issue #379: migrates the database, but refuses one that has tables and no Flyway history.
 * <p>
 * Requel 1.2 and later always leave a {@code flyway_schema_history} table, and a fresh database
 * is empty. A database with tables but no history was made by Requel 1.0/1.1 (Hibernate
 * {@code ddl-auto}) or by something else entirely. Flyway used to baseline it at version 0 and
 * run V1, a mysqldump whose {@code DROP TABLE IF EXISTS} statements emptied every table without
 * a word in the log. Now startup stops with {@link UnversionedDatabaseException} and the
 * database is left as it was, unless {@value #UPGRADE_PROPERTY} is set: then a Requel 1.0.1, 1.0.2
 * or 1.1.0 database is upgraded in place first ({@link LegacySchemaUpgrader}, #380).
 */
@Component
public class RequelFlywayMigrationStrategy implements FlywayMigrationStrategy {

	/** Issue #380: opt in to upgrading a Requel 1.0.x/1.1 database in place. */
	public static final String UPGRADE_PROPERTY = "requel.upgrade.from-1x";

	private final boolean upgradeFrom1x;

	/** Refuses an unversioned database (the default). */
	public RequelFlywayMigrationStrategy() {
		this(false);
	}

	@Autowired
	public RequelFlywayMigrationStrategy(@Value("${" + UPGRADE_PROPERTY + ":false}") boolean upgradeFrom1x) {
		this.upgradeFrom1x = upgradeFrom1x;
	}

	@Override
	public void migrate(Flyway flyway) {
		DataSource dataSource = flyway.getConfiguration().getDataSource();
		String historyTable = flyway.getConfiguration().getTable();
		if (isUnversioned(dataSource, historyTable)) {
			if (!upgradeFrom1x) {
				refuseUnversionedDatabase(dataSource, historyTable);
			}
			new LegacySchemaUpgrader(dataSource).upgrade(flyway);
		}
		flyway.migrate();
	}

	/**
	 * Throws {@link UnversionedDatabaseException} when the connection's database has at least
	 * one table and none of them is {@code historyTable}.
	 */
	public static void refuseUnversionedDatabase(DataSource dataSource, String historyTable) {
		try (Connection connection = dataSource.getConnection()) {
			String database = connection.getCatalog();
			List<String> tables = tables(connection, database);
			if (isUnversioned(tables, historyTable)) {
				throw new UnversionedDatabaseException(database, tables.size());
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Could not inspect the database before migrating it", e);
		}
	}

	private static boolean isUnversioned(DataSource dataSource, String historyTable) {
		try (Connection connection = dataSource.getConnection()) {
			return isUnversioned(tables(connection, connection.getCatalog()), historyTable);
		} catch (SQLException e) {
			throw new IllegalStateException("Could not inspect the database before migrating it", e);
		}
	}

	private static boolean isUnversioned(List<String> tables, String historyTable) {
		return !tables.isEmpty() && tables.stream().noneMatch(historyTable::equalsIgnoreCase);
	}

	private static List<String> tables(Connection connection, String database) throws SQLException {
		DatabaseMetaData metaData = connection.getMetaData();
		List<String> tables = new ArrayList<>();
		try (ResultSet rs = metaData.getTables(database, connection.getSchema(), "%",
				new String[] { "TABLE" })) {
			while (rs.next()) {
				tables.add(rs.getString("TABLE_NAME"));
			}
		}
		return tables;
	}
}
