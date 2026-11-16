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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Issue #380: a schema's shape, one line per column, index and foreign key, read from MySQL's
 * {@code information_schema} and sorted, so two schemas can be compared line by line and a
 * mismatch quoted.
 *
 * <pre>
 * C|table|column|column_type|is_nullable|default|extra
 * I|table|index|non_unique|col1,col2
 * F|table|constraint|col1,col2|referenced_table|ref1,ref2
 * T|table|collation                      (with collations only)
 * K|table|column|collation               (with collations only, text columns)
 * </pre>
 *
 * Flyway's history table is left out, so V1's shape and an old release's compare directly.
 * Collations are left out of an old release's recorded shape: a 1.0.x database is latin1 and a
 * 1.1 one is whatever the server defaulted to, and the upgrade converts every table to V1's
 * utf8mb4 either way. V1's shape includes them, so that conversion is checked. The recorded
 * shapes in {@code db/legacy/*.shape} were written by {@link #main}.
 */
public final class LegacySchemaShape {

	static final String HISTORY_TABLE = "flyway_schema_history";

	private static final String COLUMNS = "SELECT table_name, column_name, column_type, is_nullable,"
			+ " column_default, extra FROM information_schema.columns WHERE table_schema = ?";
	private static final String INDEXES = "SELECT table_name, index_name, non_unique,"
			+ " GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')"
			+ " FROM information_schema.statistics WHERE table_schema = ?"
			+ " GROUP BY table_name, index_name, non_unique";
	private static final String TABLE_COLLATIONS = "SELECT table_name, table_collation"
			+ " FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE'";
	private static final String COLUMN_COLLATIONS = "SELECT table_name, column_name, collation_name"
			+ " FROM information_schema.columns WHERE table_schema = ? AND collation_name IS NOT NULL";
	private static final String FOREIGN_KEYS = "SELECT table_name, constraint_name,"
			+ " GROUP_CONCAT(column_name ORDER BY ordinal_position SEPARATOR ','), referenced_table_name,"
			+ " GROUP_CONCAT(referenced_column_name ORDER BY ordinal_position SEPARATOR ',')"
			+ " FROM information_schema.key_column_usage"
			+ " WHERE table_schema = ? AND referenced_table_name IS NOT NULL"
			+ " GROUP BY table_name, constraint_name, referenced_table_name";

	private final List<String> lines;

	private LegacySchemaShape(List<String> lines) {
		this.lines = List.copyOf(lines);
	}

	public List<String> lines() {
		return lines;
	}

	/** Reads the shape of the connection's current database, without collations. */
	public static LegacySchemaShape read(Connection connection) throws SQLException {
		return read(connection, false);
	}

	/** Reads the shape of the connection's current database. */
	public static LegacySchemaShape read(Connection connection, boolean withCollations) throws SQLException {
		String schema = connection.getCatalog();
		TreeSet<String> lines = new TreeSet<>();
		query(connection, COLUMNS, schema, 6, "C", lines);
		query(connection, INDEXES, schema, 4, "I", lines);
		query(connection, FOREIGN_KEYS, schema, 5, "F", lines);
		if (withCollations) {
			query(connection, TABLE_COLLATIONS, schema, 2, "T", lines);
			query(connection, COLUMN_COLLATIONS, schema, 3, "K", lines);
		}
		return new LegacySchemaShape(new ArrayList<>(lines));
	}

	/** Loads a recorded shape from the classpath. */
	public static LegacySchemaShape load(String resource) {
		try (InputStream in = LegacySchemaShape.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalStateException("Missing schema shape " + resource);
			}
			List<String> lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
					.filter(l -> !l.isBlank() && !l.startsWith("#"))
					.toList();
			return new LegacySchemaShape(lines);
		} catch (IOException e) {
			throw new IllegalStateException("Could not read schema shape " + resource, e);
		}
	}

	/**
	 * @return the first line that differs, as "missing: ..." (in {@code expected}, not here) or
	 *         "unexpected: ..." (here, not in {@code expected}); empty when the shapes are equal.
	 */
	public Optional<String> firstDifference(LegacySchemaShape expected) {
		TreeSet<String> missing = new TreeSet<>(expected.lines);
		missing.removeAll(lines);
		TreeSet<String> unexpected = new TreeSet<>(lines);
		unexpected.removeAll(expected.lines);
		if (missing.isEmpty() && unexpected.isEmpty()) {
			return Optional.empty();
		}
		String m = missing.isEmpty() ? null : missing.first();
		String u = unexpected.isEmpty() ? null : unexpected.first();
		if (u == null || (m != null && m.compareTo(u) <= 0)) {
			return Optional.of("missing: " + m);
		}
		return Optional.of("unexpected: " + u);
	}

	/** @return the number of lines that differ either way, to pick the closest recorded shape. */
	public int distanceTo(LegacySchemaShape other) {
		TreeSet<String> a = new TreeSet<>(lines);
		a.removeAll(other.lines);
		TreeSet<String> b = new TreeSet<>(other.lines);
		b.removeAll(lines);
		return a.size() + b.size();
	}

	/** @return true when the database has any table besides Flyway's history. */
	public boolean isEmpty() {
		return lines.isEmpty();
	}

	private static void query(Connection connection, String sql, String schema, int columns,
			String kind, TreeSet<String> into) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, schema);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if (HISTORY_TABLE.equalsIgnoreCase(rs.getString(1))) {
						continue;
					}
					StringBuilder line = new StringBuilder(kind);
					for (int i = 1; i <= columns; i++) {
						String value = rs.getString(i);
						line.append('|').append(value == null ? "" : value);
					}
					into.add(line.toString());
				}
			}
		}
	}

	/**
	 * Prints a database's shape, to record a new one:
	 * {@code java ... LegacySchemaShape jdbc:mysql://host/db user password [collations] > x.shape}.
	 */
	public static void main(String[] args) throws SQLException {
		try (Connection c = java.sql.DriverManager.getConnection(args[0], args[1], args[2])) {
			read(c, args.length > 3 && "collations".equals(args[3])).lines().forEach(System.out::println);
		}
	}
}
