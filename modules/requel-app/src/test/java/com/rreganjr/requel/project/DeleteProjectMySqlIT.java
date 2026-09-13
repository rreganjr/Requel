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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.rreganjr.requel.user.User;

/**
 * Every {@link DeleteProjectIT} case, run against a real MySQL 8.4 with the Flyway
 * schema (issue #247).
 * <p>
 * The H2 profile ({@code create-drop}, Flyway off) cannot reproduce what broke
 * DeleteProject in the e2e suite: the InnoDB foreign keys on the
 * {@code <table>_annotations} join tables, {@code usecases.scenario_id} and
 * {@code scenarios.projectordomain_id}, or the row-locking a full-scan delete on the
 * un-indexed {@code annotation_annotatable} took. This class re-runs the inherited
 * cases on the production schema so those regress here instead of in CI's e2e run.
 * <p>
 * Skipped (not failed) when Docker is not available on the build machine.
 */
@Testcontainers(disabledWithoutDocker = true)
public class DeleteProjectMySqlIT extends DeleteProjectIT {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			// the e2e/dev compose file runs 8.4 with the same character set defaults
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@DynamicPropertySource
	static void mysql(DynamicPropertyRegistry registry) {
		// DeleteProjectIT is @TestInstance(PER_CLASS): Spring builds the context while creating
		// the test instance, BEFORE the Testcontainers extension's beforeAll starts the
		// @Container. Start it here so the mapped port exists when the suppliers run
		// (start() on a running container is a no-op; the extension still stops it).
		if (!MYSQL.isRunning()) {
			MYSQL.start();
		}
		registry.add("spring.datasource.url", () -> MYSQL.getJdbcUrl()
				+ "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC");
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
		registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
		// The production path: Flyway builds the schema (V1..Vn), Hibernate only maps it.
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.flyway.baseline-on-migrate", () -> "true");
		registry.add("spring.flyway.baseline-version", () -> "0");
		// Fail fast, visibly, if a lock is ever contended in this single-threaded test.
		registry.add("spring.datasource.hikari.connection-init-sql",
				() -> "SET SESSION innodb_lock_wait_timeout = 10");
	}

	@Autowired
	private JdbcTemplate mysqlJdbcTemplate;

	/**
	 * #279, the real race. {@code DeleteProjectCommandImpl} deletes children first and the
	 * project row last, so its natural lock order is children -> project; an apply that
	 * locked the project and then wrote annotations would order them project -> children,
	 * which is an ABBA deadlock under InnoDB. The fix makes the {@code pods} row a single
	 * ordered gate both paths take <em>first</em>.
	 * <p>
	 * This holds the gate on one thread and starts the delete on another, then proves the
	 * delete really is blocked by it before releasing. If the lock were not taken - or were
	 * taken in the wrong order - this fails fast rather than hanging: the class pins
	 * {@code innodb_lock_wait_timeout} to 10s.
	 */
	@Test
	void projectDeleteBlocksOnTheSameGateAnAssistantApplyTakes() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		long ts = System.currentTimeMillis();
		Project project = createProject(admin, "del-race-" + ts);
		Long projectId = project.getId();

		CountDownLatch gateHeld = new CountDownLatch(1);
		CountDownLatch releaseGate = new CountDownLatch(1);
		ExecutorService threads = Executors.newFixedThreadPool(2);
		try {
			// Thread A: take the gate the way an apply does, and hold it.
			Future<?> holder = threads.submit(() -> {
				new org.springframework.transaction.support.TransactionTemplate(transactionManager)
						.execute(status -> {
							mysqlJdbcTemplate.queryForList(
									"SELECT id FROM pods WHERE id = ? FOR UPDATE", projectId);
							gateHeld.countDown();
							try {
								releaseGate.await(30, TimeUnit.SECONDS);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
							return null;
						});
				return null;
			});
			assertTrue(gateHeld.await(30, TimeUnit.SECONDS), "thread A never took the gate");

			// Thread B: delete the project. It must block on the very same row.
			Integer expectedVersion = project.getVersion();
			Future<?> deleter = threads.submit(() -> {
				deleteProject(admin, project, expectedVersion);
				return null;
			});
			assertFalse(isDone(deleter), "the delete must block on the project row while an "
					+ "assistant apply holds it; it did not, so the gate is not being taken "
					+ "before the cascade");

			releaseGate.countDown();
			deleter.get(60, TimeUnit.SECONDS);
			holder.get(30, TimeUnit.SECONDS);
		} finally {
			threads.shutdownNow();
		}

		assertEquals(0, mysqlJdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM pods WHERE id = ?", Integer.class, projectId),
				"the delete must still complete once the gate is released");
		assertEquals(0, mysqlJdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM annotations WHERE grouping_object_id = ?", Integer.class,
				projectId),
				"no annotation may survive grouped under the deleted project");
	}

	/**
	 * #279: V17 collects annotations already orphaned by this bug. Flyway has run it against
	 * an empty schema by the time these tests execute, so it cleaned nothing; seed the shape
	 * it targets and re-run its statements.
	 * <p>
	 * The interesting case is the shared position: {@code PositionImpl.issues} is a
	 * {@code @ManyToMany}, so a position answering a live issue as well as an orphan one must
	 * survive.
	 */
	@Test
	void v17CleansOrphanAnnotationsButKeepsPositionsAnsweringLiveIssues() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		long ts = System.currentTimeMillis();
		Long adminId = admin.getId();
		long missingProjectId = 900000000L + (ts % 1000000L);

		// A live project with a real issue, and an orphan issue grouped under a pods row
		// that does not exist - exactly what a losing assistant apply used to leave behind.
		Project live = createProject(admin, "del-v17-" + ts);
		Long liveIssueId = insertAnnotation(adminId, live.getId(), "live issue " + ts);
		Long orphanIssueId = insertAnnotation(adminId, missingProjectId, "orphan issue " + ts);

		Long sharedPositionId = insertPosition(adminId, "shared position " + ts);
		Long orphanOnlyPositionId = insertPosition(adminId, "orphan-only position " + ts);
		mysqlJdbcTemplate.update("INSERT INTO position_issue (position_id, issue_id) VALUES (?, ?)",
				sharedPositionId, liveIssueId);
		mysqlJdbcTemplate.update("INSERT INTO position_issue (position_id, issue_id) VALUES (?, ?)",
				sharedPositionId, orphanIssueId);
		mysqlJdbcTemplate.update("INSERT INTO position_issue (position_id, issue_id) VALUES (?, ?)",
				orphanOnlyPositionId, orphanIssueId);
		mysqlJdbcTemplate.update(
				"INSERT INTO annotation_annotatable (annotation_id, annotatable_type, annotatable_id)"
						+ " VALUES (?, 'Goal', 999999999)",
				orphanIssueId);

		runV17();

		assertEquals(0, mysqlJdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM annotations WHERE id = ?", Integer.class, orphanIssueId),
				"the orphan annotation must be collected");
		assertEquals(0, mysqlJdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM annotation_annotatable WHERE annotation_id = ?",
				Integer.class, orphanIssueId),
				"its link rows must go with it");
		assertEquals(0, mysqlJdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM positions WHERE id = ?", Integer.class, orphanOnlyPositionId),
				"a position that only answered the orphan issue must go too");
		assertEquals(1, mysqlJdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM annotations WHERE id = ?", Integer.class, liveIssueId),
				"the live issue must be untouched");
		assertEquals(1, mysqlJdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM positions WHERE id = ?", Integer.class, sharedPositionId),
				"a position still answering a live issue must survive - positions are shared");
	}

	/** A Future is "not done" only if it is still running - a thrown task counts as done. */
	private static boolean isDone(Future<?> future) throws Exception {
		// Give the delete a moment to reach the lock before concluding it is blocked.
		Thread.sleep(1500);
		return future.isDone();
	}

	private Long insertAnnotation(Long createdById, Long groupingObjectId, String text) {
		mysqlJdbcTemplate.update("INSERT INTO annotations (annotation_type, date_created,"
				+ " grouping_object_type, grouping_object_id, text, version, must_be_resolved,"
				+ " created_by_id) VALUES ('com.rreganjr.requel.annotation.Issue', NOW(6),"
				+ " 'Project', ?, ?, 0, b'0', ?)", groupingObjectId, text, createdById);
		return mysqlJdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}

	private Long insertPosition(Long createdById, String text) {
		mysqlJdbcTemplate.update("INSERT INTO positions (position_type, date_created, text,"
				+ " version, created_by_id)"
				+ " VALUES ('com.rreganjr.requel.annotation.impl.PositionImpl', NOW(6), ?, 0, ?)",
				text, createdById);
		return mysqlJdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}

	/**
	 * Re-run V17's statements. Flyway applied them to an empty schema at context startup, so
	 * running them again is the only way to exercise what they actually collect. Reads the
	 * real migration off the classpath rather than restating it, so the test cannot drift
	 * away from what ships.
	 */
	private void runV17() throws Exception {
		String sql;
		try (java.io.InputStream in = getClass()
				.getResourceAsStream("/db/migration/V17__delete_orphan_annotations.sql")) {
			org.junit.jupiter.api.Assertions.assertNotNull(in, "V17 migration not on the classpath");
			sql = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
		StringBuilder stripped = new StringBuilder();
		for (String line : sql.split("\\R")) {
			if (!line.trim().startsWith("--")) {
				stripped.append(line).append('\n');
			}
		}
		for (String statement : stripped.toString().split(";")) {
			if (!statement.isBlank()) {
				mysqlJdbcTemplate.execute(statement.trim());
			}
		}
	}
}
