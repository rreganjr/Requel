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
package com.rreganjr.requel.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * #392: V38 gives the built-in admin ProjectUserRole.manageApiTokens on a database where admin
 * already has the role (an existing install), creating the permission row if startup hasn't yet,
 * and leaves every other user alone.
 */
@Testcontainers(disabledWithoutDocker = true)
public class AdminApiTokensMigrationMySqlIT {

	private static final String PROJECT_ROLE = "com.rreganjr.requel.project.ProjectUserRole";

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@Test
	void v38GrantsManageApiTokensToAdminOnly() throws Exception {
		migrateTo("37");
		try (Connection c = connect()) {
			exec(c, "SET FOREIGN_KEY_CHECKS = 0");
			exec(c, "INSERT INTO user_role_permissions (id, name, role_type) VALUES (1, 'createProjects', '"
					+ PROJECT_ROLE + "')");
			user(c, 10, "admin");
			user(c, 11, "project");
			exec(c, "INSERT INTO user_roles (id, role_type, version, user_id) VALUES (20, '" + PROJECT_ROLE + "', 0, 10)");
			exec(c, "INSERT INTO user_roles (id, role_type, version, user_id) VALUES (21, '" + PROJECT_ROLE + "', 0, 11)");
			exec(c, "INSERT INTO user_roles_permissions (user_role_id, user_role_permission_id) VALUES (20, 1), (21, 1)");
		}

		migrateTo(null);

		try (Connection c = connect()) {
			assertEquals(1, count(c, 20, "manageApiTokens"), "admin gets manageApiTokens");
			assertEquals(1, count(c, 20, "createProjects"), "admin keeps createProjects");
			assertEquals(0, count(c, 21, "manageApiTokens"), "other users are left alone");
			assertEquals(1, scalar(c, "SELECT COUNT(*) FROM user_role_permissions WHERE name = 'manageApiTokens'"),
					"the permission row is created once");
		}
	}

	private static void user(Connection c, long id, String username) throws SQLException {
		exec(c, "INSERT INTO users (id, editable, email_address, hashed_password, username, version, organization_id)"
				+ " VALUES (" + id + ", b'1', '" + username + "@example.invalid', 'x', '" + username + "', 0, 1)");
	}

	private static int count(Connection c, long roleId, String permission) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(
				"SELECT COUNT(*) FROM user_roles_permissions rp JOIN user_role_permissions p"
						+ " ON p.id = rp.user_role_permission_id WHERE rp.user_role_id = ? AND p.name = ?")) {
			ps.setLong(1, roleId);
			ps.setString(2, permission);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getInt(1);
			}
		}
	}

	private static int scalar(Connection c, String sql) throws SQLException {
		try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
			rs.next();
			return rs.getInt(1);
		}
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
