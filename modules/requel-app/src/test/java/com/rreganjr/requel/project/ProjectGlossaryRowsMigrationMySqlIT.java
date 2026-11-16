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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * #480: V44 puts back the {@code pods_glossary_terms} row of a term that lost it to a 2.1 rename,
 * from the term's own {@code projectordomain_id}, and leaves the rows that are there alone.
 */
@Testcontainers(disabledWithoutDocker = true)
public class ProjectGlossaryRowsMigrationMySqlIT {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@Test
	void v44RestoresTheGlossaryRowsOfTermsThatLostThem() throws Exception {
		migrateTo("43");
		try (Connection c = connect()) {
			exec(c, "SET FOREIGN_KEY_CHECKS = 0");
			exec(c, "INSERT INTO pods (type, id, name, version) VALUES"
					+ " ('com.rreganjr.requel.project.impl.ProjectImpl', 1, 'Requel', 1),"
					+ " ('com.rreganjr.requel.project.impl.ProjectImpl', 2, 'Other', 1)");
			exec(c, "INSERT INTO terms (id, version, name, created_by_id, projectordomain_id, canonical_term_id) VALUES"
					+ " (1, 1, 'Yellow Snow', 1, 1, NULL),"
					+ " (2, 2, 'Zoiniks', 1, 1, 1),"
					+ " (3, 2, 'Pee Pee Snow Cone', 1, 1, 1),"
					+ " (4, 1, 'Ledger', 1, 2, NULL)");
			exec(c, "INSERT INTO pods_glossary_terms (abstract_project_or_domain_id, glossary_terms_id)"
					+ " VALUES (1, 2), (1, 3), (2, 4)");
		}

		migrateTo(null);

		assertEquals(List.of("1:1", "1:2", "1:3", "2:4"), rows(),
				"term 1 is back in its project's glossary, and no row is duplicated or moved");
	}

	private static List<String> rows() throws Exception {
		List<String> rows = new ArrayList<>();
		try (Connection c = connect();
				Statement s = c.createStatement();
				ResultSet rs = s.executeQuery("SELECT abstract_project_or_domain_id, glossary_terms_id"
						+ " FROM pods_glossary_terms ORDER BY glossary_terms_id")) {
			while (rs.next()) {
				rows.add(rs.getLong(1) + ":" + rs.getLong(2));
			}
		}
		return rows;
	}

	private static void exec(Connection c, String sql) throws SQLException {
		try (Statement s = c.createStatement()) {
			s.execute(sql);
		}
	}

	private static void migrateTo(String version) {
		var config = Flyway.configure()
				.dataSource(url(), MYSQL.getUsername(), MYSQL.getPassword())
				.locations("classpath:db/migration");
		if (version != null) {
			config = config.target(version);
		}
		config.load().migrate();
	}

	private static Connection connect() throws Exception {
		return DriverManager.getConnection(url(), MYSQL.getUsername(), MYSQL.getPassword());
	}

	private static String url() {
		return MYSQL.getJdbcUrl() + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
	}
}
