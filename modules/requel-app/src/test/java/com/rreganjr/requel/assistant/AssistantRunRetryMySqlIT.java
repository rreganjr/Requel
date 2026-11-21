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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.AppliedAssistantResult;
import com.rreganjr.requel.assistant.core.AssistantProjectGate;
import com.rreganjr.requel.assistant.core.AssistantResultApplicator;
import com.rreganjr.requel.assistant.core.AssistantRunRecord;
import com.rreganjr.requel.assistant.core.AssistantRunStatus;
import com.rreganjr.requel.assistant.core.AssistantRunStore;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.AssistantTargetLoader;
import com.rreganjr.requel.assistant.core.SimpleAssistantRegistry;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;

/**
 * Issue #495: an assistant run's apply phase loses a race - a deadlock, a stale version, a
 * foreign key to a row deleted under it - and is retried instead of failing the run, on the
 * production schema (Flyway, InnoDB) where those failures happen.
 * <p>
 * The worker is built here around the application's own run store, applicator, target loaders,
 * project gate and transaction manager, with a test assistant that offers one issue and the
 * shared "Do nothing." position on a goal. Its applicator is wrapped so a test can act from
 * another connection at the moment the apply transaction is open, which is what manufactures
 * the race deterministically.
 * <p>
 * Skipped (not failed) when Docker is not available on the build machine.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AssistantRunRetryMySqlIT extends AbstractIntegrationTestCase {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@DynamicPropertySource
	static void mysql(DynamicPropertyRegistry registry) {
		// PER_CLASS: Spring builds the context before the Testcontainers extension starts the
		// @Container, so start it here (see DeleteProjectMySqlIT).
		if (!MYSQL.isRunning()) {
			MYSQL.start();
		}
		registry.add("spring.datasource.url", () -> MYSQL.getJdbcUrl()
				+ "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC");
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
		registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.flyway.baseline-on-migrate", () -> "true");
		registry.add("spring.flyway.baseline-version", () -> "0");
		// A blocked statement fails in seconds rather than hanging the build.
		registry.add("spring.datasource.hikari.connection-init-sql",
				() -> "SET SESSION innodb_lock_wait_timeout = 10");
	}

	private static final String DO_NOTHING = "Do nothing (#495 retry test).";

	@Autowired
	private AssistantRunStore runStore;
	@Autowired
	private AssistantResultApplicator applicator;
	@Autowired
	private List<AssistantTargetLoader> targetLoaders;
	@Autowired
	private AssistantProjectGate projectGate;
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private JdbcTemplate jdbc;

	/** Another connection, for acting while a run's apply transaction is open. */
	private final ExecutorService otherConnection = Executors.newCachedThreadPool();

	@AfterAll
	void stopThreads() {
		otherConnection.shutdownNow();
	}

	/**
	 * Runs on different goals of one project, all offering the project's shared "Do nothing."
	 * position, started together and repeated. Every run applies its findings, and the shared
	 * position ends up on every issue.
	 */
	@Test
	void concurrentRunsOfferingTheSamePositionAllApplyTheirFindings() throws Exception {
		User creator = getUserRepository().findUserByUsername("project");
		Project project = createProject(creator, "retry-shared");
		AssistantRunWorker worker = worker(applicator);

		List<Goal> goals = new ArrayList<>();
		goals.add(createGoal(creator, project, "first"));
		// The first run creates the shared position; every later one updates it.
		assertEquals(AssistantRunStatus.SUCCEEDED, run(worker, goals.get(0)));

		ExecutorService runners = Executors.newFixedThreadPool(4);
		try {
			for (int round = 0; round < 3; round++) {
				List<Goal> roundGoals = new ArrayList<>();
				for (int i = 0; i < 4; i++) {
					roundGoals.add(createGoal(creator, project, "round " + round + " goal " + i));
				}
				List<UUID> runIds = roundGoals.stream().map(goal -> queue(project, goal)).toList();
				CountDownLatch start = new CountDownLatch(1);
				List<Future<?>> futures = new ArrayList<>();
				for (UUID runId : runIds) {
					futures.add(runners.submit(() -> {
						start.await();
						worker.run(runId);
						return null;
					}));
				}
				start.countDown();
				for (Future<?> future : futures) {
					future.get(120, TimeUnit.SECONDS);
				}
				for (UUID runId : runIds) {
					assertEquals(AssistantRunStatus.SUCCEEDED, status(runId),
							"every concurrent run must apply its findings");
				}
				goals.addAll(roundGoals);
			}
		} finally {
			runners.shutdownNow();
		}

		for (Goal goal : goals) {
			assertEquals(1, issuesOn(goal), "goal " + goal.getId() + " lost its finding");
		}
		assertEquals(goals.size(), jdbc.queryForObject(
				"SELECT COUNT(*) FROM position_issue pi JOIN positions p ON p.id = pi.position_id"
						+ " JOIN annotations i ON i.id = pi.issue_id"
						+ " WHERE p.text = ? AND i.grouping_object_id = ?",
				Integer.class, DO_NOTHING, project.getId()),
				"the shared position must be on every run's issue");
	}

	/**
	 * Another connection changes the shared position while the apply transaction has it in
	 * hand, so the apply's write meets a newer version. The attempt fails, the retry re-reads
	 * the position and the run succeeds.
	 */
	@Test
	void aPositionChangedDuringTheApplyIsRetried() throws Exception {
		User creator = getUserRepository().findUserByUsername("project");
		Project project = createProject(creator, "retry-stale");
		assertEquals(AssistantRunStatus.SUCCEEDED,
				run(worker(applicator), createGoal(creator, project, "first")));
		Long positionId = jdbc.queryForObject("SELECT DISTINCT p.id FROM positions p"
				+ " JOIN position_issue pi ON pi.position_id = p.id"
				+ " JOIN annotations i ON i.id = pi.issue_id"
				+ " WHERE p.text = ? AND i.grouping_object_id = ?", Long.class, DO_NOTHING,
				project.getId());

		AtomicBoolean changed = new AtomicBoolean();
		int[] attempts = new int[1];
		AssistantResultApplicator changingAfterRead = new AssistantResultApplicator() {
			@Override
			public AppliedAssistantResult apply(AssistantContext context, AssistantResult result,
					CleanupPolicy cleanupPolicy, EntityRef dispatchTarget) {
				attempts[0]++;
				AppliedAssistantResult applied = applicator.apply(context, result, cleanupPolicy,
						dispatchTarget);
				if (changed.compareAndSet(false, true)) {
					onAnotherConnection(() -> jdbc.update(
							"UPDATE positions SET version = version + 1 WHERE id = ?", positionId));
				}
				return applied;
			}
		};
		Goal goal = createGoal(creator, project, "second");

		assertEquals(AssistantRunStatus.SUCCEEDED, run(worker(changingAfterRead), goal));
		assertEquals(2, attempts[0], "the stale apply must be retried once");
		assertEquals(1, issuesOn(goal));
	}

	/**
	 * The goal is deleted from another connection while the apply transaction is open, so the
	 * apply's insert fails its foreign key. The retry finds the goal gone and the run is
	 * skipped - not failed.
	 */
	@Test
	void aTargetDeletedDuringTheApplyIsSkipped() throws Exception {
		User creator = getUserRepository().findUserByUsername("project");
		Project project = createProject(creator, "retry-deleted");
		Goal goal = createGoal(creator, project, "doomed");

		AtomicBoolean deleted = new AtomicBoolean();
		AssistantResultApplicator deletingFirst = new AssistantResultApplicator() {
			@Override
			public AppliedAssistantResult apply(AssistantContext context, AssistantResult result,
					CleanupPolicy cleanupPolicy, EntityRef dispatchTarget) {
				if (deleted.compareAndSet(false, true)) {
					// A user's delete, reduced to the rows a fresh goal has.
					onAnotherConnection(() -> new TransactionTemplate(transactionManager)
							.executeWithoutResult(status -> {
								jdbc.update("DELETE FROM goals_goalcontainers WHERE goal_id = ?",
										goal.getId());
								jdbc.update("DELETE FROM goals WHERE id = ?", goal.getId());
							}));
				}
				return applicator.apply(context, result, cleanupPolicy, dispatchTarget);
			}
		};

		assertEquals(AssistantRunStatus.SKIPPED, run(worker(deletingFirst), goal));
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM goals_annotations"
				+ " WHERE goal_impl_id = ?", Integer.class, goal.getId()));
	}

	// -------------------------------------------------------------------------

	private AssistantRunWorker worker(AssistantResultApplicator resultApplicator) {
		return new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new SharedPositionAssistant())),
				resultApplicator, targetLoaders, projectGate, transactionManager);
	}

	private AssistantRunStatus run(AssistantRunWorker worker, Goal goal) {
		UUID runId = queue(goal.getProjectOrDomain(), goal);
		worker.run(runId);
		return status(runId);
	}

	private UUID queue(com.rreganjr.requel.project.ProjectOrDomain project, Goal goal) {
		User creator = getUserRepository().findUserByUsername("project");
		User assistant = getUserRepository().findUserByUsername("assistant");
		AnalysisRequest request = new AnalysisRequest(EntityRef.of("Goal", goal.getId()),
				EntityRef.of("Project", project.getId()),
				new UserRef(creator.getId(), creator.getUsername()),
				new UserRef(assistant.getId(), assistant.getUsername()), TASK_TYPE,
				Locale.getDefault(), Map.of());
		return runStore.queueRun(request).runId();
	}

	private AssistantRunStatus status(UUID runId) {
		return runStore.findRun(runId).map(AssistantRunRecord::status)
				.orElseThrow(() -> new AssertionError("run " + runId + " vanished"));
	}

	private int issuesOn(Goal goal) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM goals_annotations ga"
				+ " JOIN annotations a ON a.id = ga.annotations_id"
				+ " WHERE ga.goal_impl_id = ? AND a.text LIKE '#495 finding%'", Integer.class,
				goal.getId());
	}

	/** Runs {@code work} on a connection of its own and waits for it to commit. */
	private void onAnotherConnection(Runnable work) {
		try {
			otherConnection.submit(work).get(30, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new IllegalStateException("the other connection's change failed: "
					+ (e.getCause() == null ? e : e.getCause()), e);
		}
	}

	private Project createProject(User creator, String name) throws Exception {
		long ts = System.nanoTime();
		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(creator);
		command.setName(name + " " + ts);
		command.setOrganizationName("Org " + ts);
		return getCommandHandler().execute(command).getProject();
	}

	private Goal createGoal(User creator, Project project, String name) throws Exception {
		EditGoalCommand command = getProjectCommandFactory().newEditGoalCommand();
		command.setEditedBy(creator);
		command.setGoalContainer(project);
		command.setName(name + " " + System.nanoTime());
		command.setText("A goal for the #495 retry test.");
		return getCommandHandler().execute(command).getGoal();
	}

	/** The task type the test runs use, so the goals' own queued analysis is never picked up. */
	private static final String TASK_TYPE = "RETRY_TEST_495";

	/** One issue per goal, with the project-wide "Do nothing." position every issue shares. */
	private static final class SharedPositionAssistant implements RequelAssistant<Goal> {
		@Override
		public String assistantId() {
			return "retry-test-495";
		}

		@Override
		public Class<Goal> targetType() {
			return Goal.class;
		}

		@Override
		public boolean handlesTask(String taskType) {
			return TASK_TYPE.equals(taskType);
		}

		@Override
		public AssistantResult analyze(AssistantContext context, Goal goal) {
			EntityRef targetRef = EntityRef.of("Goal", goal.getId());
			String key = assistantId() + ":Goal:" + goal.getId();
			return AssistantResult.builder().assistantId(assistantId()).runId(context.runId())
					.summary("#495 retry test")
					.annotationAction(new AnnotationAction(key,
							AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null,
							"#495 finding on goal " + goal.getId(), "LOW", null, List.of(),
							Map.of("mustResolve", Boolean.FALSE)))
					.annotationAction(new AnnotationAction(key + ":do-nothing",
							AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key,
							DO_NOTHING, null, null, List.of(), Map.of()))
					.build();
		}
	}
}
