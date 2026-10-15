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

/**
 * Issue #379: the database has tables but no Flyway history, so Requel will not migrate it.
 * {@link RequelStartupFailureAnalyzer} turns it into the startup failure report.
 */
public class UnversionedDatabaseException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	private final String database;
	private final int tableCount;

	public UnversionedDatabaseException(String database, int tableCount) {
		super("Database `" + database + "` has " + tableCount + " tables but no Flyway history,"
				+ " so it was not created by Requel 1.2 or later. Requel will not modify it.");
		this.database = database;
		this.tableCount = tableCount;
	}

	public String getDatabase() {
		return database;
	}

	public int getTableCount() {
		return tableCount;
	}
}
