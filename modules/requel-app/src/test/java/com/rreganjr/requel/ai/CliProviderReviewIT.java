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
package com.rreganjr.requel.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.assistant.ai.AiAnalysisClient;
import com.rreganjr.requel.assistant.ai.cli.CliAiAnalysisClient;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunRepository;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageRepository;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;

/**
 * #259 end to end with the {@code cli} provider selected, against a <em>fake</em> CLI: a shell
 * script that drains stdin and prints whatever {@code reply.json} beside it holds, or fails when a
 * {@code fail} file is present. No real {@code claude}/{@code codex} binary is involved.
 *
 * <p>
 * The provider properties are a {@code @TestPropertySource} (not the {@code ai-cli} profile) because
 * the base class pins {@code requel.ai.provider=noop} there, which outranks a profile file. The
 * command path is a {@code @DynamicPropertySource}; {@code CliAiProperties} is
 * {@code @ConfigurationProperties}, so it sees it (#293 only bites {@code @Value}).
 */
@EnabledOnOs({ OS.LINUX, OS.MAC })
@TestPropertySource(properties = {
		"requel.ai.enabled=true",
		"requel.ai.provider=cli",
		"requel.ai.model=claude-cli",
		"spring.ai.model.chat=none",
		"requel.ai.cli.output-format=claude-json",
		"requel.ai.cli.timeout=30s" })
public class CliProviderReviewIT extends AbstractIntegrationTestCase {

	private static final Path FAKE_CLI_DIR = createFakeCli();

	@DynamicPropertySource
	static void cliCommand(DynamicPropertyRegistry registry) {
		registry.add("requel.ai.cli.command", () -> FAKE_CLI_DIR.resolve("fake-claude").toString());
	}

	private AiReviewService aiReviewService;
	private AssistantRunWorker assistantRunWorker;
	private AssistantRunRepository assistantRunRepository;
	private AssistantUsageRepository assistantUsageRepository;
	private AiAnalysisClient aiAnalysisClient;
	private JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	protected void setDependencies(AiReviewService aiReviewService,
			AssistantRunWorker assistantRunWorker, AssistantRunRepository assistantRunRepository,
			AssistantUsageRepository assistantUsageRepository, AiAnalysisClient aiAnalysisClient,
			JdbcTemplate jdbcTemplate) {
		this.aiReviewService = aiReviewService;
		this.assistantRunWorker = assistantRunWorker;
		this.assistantRunRepository = assistantRunRepository;
		this.assistantUsageRepository = assistantUsageRepository;
		this.aiAnalysisClient = aiAnalysisClient;
		this.jdbcTemplate = jdbcTemplate;
	}

	@AfterEach
	public void reset() throws IOException {
		SecurityContextHolder.clearContext();
		Files.deleteIfExists(FAKE_CLI_DIR.resolve("fail"));
		Files.deleteIfExists(FAKE_CLI_DIR.resolve("reply.json"));
	}

	@Test
	public void theCliClientIsTheOnlyProvider() {
		assertTrue(aiAnalysisClient instanceof CliAiAnalysisClient,
				"requel.ai.provider=cli should select the CLI client, got "
						+ aiAnalysisClient.getClass().getName());
	}

	@Test
	public void aReviewWritesTheCliFindingsWithRunAndUsageRows() throws Exception {
		long ts = System.currentTimeMillis();
		String issueText = "'fast' is not measurable (" + ts + ")";
		String reply = objectMapper.writeValueAsString(Map.of(
				"summary", "one finding",
				"findings", List.of(Map.of(
						"findingType", "UNTESTABLE",
						"severity", "HIGH",
						"confidence", 0.8,
						"evidenceReferences", List.of("fast"),
						"suggestedIssueText", issueText,
						"suggestedPositions", List.of("Set a p95 latency budget (" + ts + ")"))),
				"warnings", List.of()));
		Files.writeString(FAKE_CLI_DIR.resolve("reply.json"), objectMapper.writeValueAsString(
				Map.of("type", "result", "is_error", false, "result", reply,
						"total_cost_usd", 0.0042,
						"usage", Map.of("input_tokens", 900, "output_tokens", 120,
								"cache_read_input_tokens", 300))));
		Goal goal = createGoal(ts);

		AssistantRunEntity completed = review(goal);

		assertEquals("SUCCEEDED", completed.getStatus(), "run summary: " + completed.getErrorSummary());
		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM annotations WHERE text LIKE ?", Integer.class,
				"%not measurable (" + ts + ")%"),
				"the CLI's finding should be written as one issue annotation");
		List<AssistantUsageEntity> usage = assistantUsageRepository
				.findByRunId(completed.getRunId().toString());
		assertEquals(1, usage.size(), "one assistant_usages row for the review run");
		assertEquals("cli", usage.get(0).getProvider());
		assertEquals("claude-cli", usage.get(0).getModel());
		assertEquals(1200, usage.get(0).getInputTokens()); // 900 uncached + 300 cache read
		assertNotNull(usage.get(0).getCostEstimate());
		assertEquals(0, new BigDecimal("0.0042").compareTo(usage.get(0).getCostEstimate()));
	}

	@Test
	public void aFailingCliFailsTheRunAndWritesNothing() throws Exception {
		long ts = System.currentTimeMillis();
		Files.writeString(FAKE_CLI_DIR.resolve("fail"), "");
		Goal goal = createGoal(ts);

		AssistantRunEntity completed = review(goal);

		assertEquals("FAILED", completed.getStatus());
		assertNotNull(completed.getErrorSummary());
		assertTrue(completed.getErrorSummary().contains("exited with status 2"),
				"error summary names the exit: " + completed.getErrorSummary());
		assertTrue(completed.getErrorSummary().contains("not logged in"),
				"error summary carries the stderr excerpt: " + completed.getErrorSummary());
		assertEquals(0, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM annotation_annotatable WHERE annotatable_type = 'Goal' "
						+ "AND annotatable_id = ?", Integer.class, goal.getId()),
				"a failed review writes no annotations");
		assertTrue(assistantUsageRepository.findByRunId(completed.getRunId().toString()).isEmpty(),
				"a failed call records no usage");
	}

	// ---- helpers ---------------------------------------------------------------------

	private Goal createGoal(long ts) throws Exception {
		User creator = getUserRepository().findUserByUsername("project");

		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(creator);
		projectCommand.setName("CliReview Project " + ts);
		projectCommand.setOrganizationName("CliReview Org " + ts);
		projectCommand = getCommandHandler().execute(projectCommand);
		Project project = projectCommand.getProject();

		EditGoalCommand goalCommand = getProjectCommandFactory().newEditGoalCommand();
		goalCommand.setEditedBy(creator);
		goalCommand.setGoalContainer(project);
		goalCommand.setName("Fast search " + ts);
		goalCommand.setText("Search results should be fast. $(touch /tmp/requel-pwned) `id`");
		goalCommand = getCommandHandler().execute(goalCommand);
		return goalCommand.getGoal();
	}

	/** Dispatch a review as the project creator, run it, and return the finished run row. */
	private AssistantRunEntity review(Goal goal) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("project", null, List.of()));
		aiReviewService.requestReview("Goal", goal.getId());

		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(run -> "Goal".equals(run.getTargetType())
						&& goal.getId().equals(run.getTargetId())
						&& "REQUIREMENTS_REVIEW".equals(run.getTaskType())
						&& "QUEUED".equals(run.getStatus()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no QUEUED review run for the goal"));

		try {
			assistantRunWorker.run(queued.getRunId());
		} catch (RuntimeException e) {
			// A run the worker marks FAILED returns normally; anything thrown is a test failure.
			throw new AssertionError("the worker should record the failure, not throw", e);
		}
		return assistantRunRepository.findById(queued.getId())
				.orElseThrow(() -> new AssertionError("review run row vanished"));
	}

	private static Path createFakeCli() {
		try {
			Path dir = Files.createTempDirectory("requel-fake-cli-");
			dir.toFile().deleteOnExit();
			Path script = dir.resolve("fake-claude");
			Files.writeString(script, String.join("\n",
					"#!/bin/sh",
					"# Fake claude for CliProviderReviewIT: drain the prompt, then reply or fail.",
					"here=$(dirname \"$0\")",
					"cat > /dev/null",
					"if [ -f \"$here/fail\" ]; then",
					"  echo 'Error: not logged in' >&2",
					"  exit 2",
					"fi",
					"cat \"$here/reply.json\"",
					""));
			Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
			return dir;
		} catch (IOException e) {
			throw new IllegalStateException("could not create the fake CLI", e);
		}
	}
}
