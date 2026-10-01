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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunReadService.RunView;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunRepository;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;

/**
 * #355: the review read ({@link AiReviewService#latestReview}) returns the newest review run on an
 * entity with the model's summary and only the findings that run reported. The {@code cli}
 * provider runs a fake CLI that prints whatever reply the test wrote beside it.
 */
@EnabledOnOs({ OS.LINUX, OS.MAC })
@TestPropertySource(properties = {
		"requel.ai.enabled=true",
		"requel.ai.provider=cli",
		"requel.ai.model=claude-cli",
		"spring.ai.model.chat=none",
		"requel.ai.cli.output-format=claude-json",
		"requel.ai.cli.timeout=30s" })
public class AiReviewReadIT extends AbstractIntegrationTestCase {

	private static final Path FAKE_CLI_DIR = createFakeCli();

	@DynamicPropertySource
	static void cliCommand(DynamicPropertyRegistry registry) {
		registry.add("requel.ai.cli.command", () -> FAKE_CLI_DIR.resolve("fake-claude").toString());
	}

	private AiReviewService aiReviewService;
	private AssistantRunWorker assistantRunWorker;
	private AssistantRunRepository assistantRunRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	protected void setDependencies(AiReviewService aiReviewService,
			AssistantRunWorker assistantRunWorker, AssistantRunRepository assistantRunRepository) {
		this.aiReviewService = aiReviewService;
		this.assistantRunWorker = assistantRunWorker;
		this.assistantRunRepository = assistantRunRepository;
	}

	@AfterEach
	public void reset() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void anEntityNeverReviewedHasNoReview() throws Exception {
		Goal goal = createGoal("Never reviewed");
		asProjectUser();
		assertTrue(aiReviewService.latestReview("Goal", goal.getId()).isEmpty());
	}

	@Test
	public void theReadReturnsTheRunsSummaryAndFindings() throws Exception {
		Goal goal = createGoal("Read one run");
		reply("The goal is vague and has no measure.",
				finding("AMBIGUOUS", "MEDIUM", "Define what 'fast' means.", null),
				finding("UNTESTABLE", "HIGH", "Add a measurable threshold.", null),
				finding("CONTEXT", null, null, "Owned by the lending desk."));

		review(goal);
		RunView view = latest(goal);

		assertEquals("SUCCEEDED", view.status(), "error: " + view.errorSummary());
		assertEquals("The goal is vague and has no measure.", view.resultSummary());
		assertEquals(3, view.findings().size(), view.findings().toString());
		Map<String, String> kindByType = view.findings().stream().collect(
				Collectors.toMap(f -> f.findingType(), f -> String.valueOf(f.kind())));
		assertEquals(Map.of("AMBIGUOUS", "ISSUE", "UNTESTABLE", "ISSUE", "CONTEXT", "NOTE"),
				kindByType);
		assertTrue(view.findings().stream()
				.anyMatch(f -> "Add a measurable threshold.".equals(f.text())
						&& "HIGH".equals(f.severity()) && f.annotationId() != null));
	}

	@Test
	public void aLaterRunReplacesTheEarlierOneInTheRead() throws Exception {
		Goal goal = createGoal("Two runs");
		reply("first", finding("AMBIGUOUS", "LOW", "First run's finding.", null));
		review(goal);
		String firstRun = latest(goal).runId();

		reply("second", finding("INCOMPLETE", "LOW", "Second run's finding.", null));
		review(goal);
		RunView second = latest(goal);

		assertNotEquals(firstRun, second.runId());
		assertEquals("second", second.resultSummary());
		assertEquals(Set.of("Second run's finding."),
				second.findings().stream().map(f -> f.text()).collect(Collectors.toSet()));
	}

	@Test
	public void aReplyThatIsNotJsonReadsAsAFailedRun() throws Exception {
		Goal goal = createGoal("Bad reply");
		writeReply("this is not JSON");

		review(goal);
		RunView view = latest(goal);

		assertEquals("FAILED", view.status());
		assertTrue(view.errorSummary().contains("model reply"), view.errorSummary());
		assertNull(view.resultSummary());
		assertTrue(view.findings().isEmpty());
	}

	@Test
	public void theReadHasThePostsChecks() throws Exception {
		Goal goal = createGoal("Checks");
		asProjectUser();
		assertThrows(IllegalArgumentException.class,
				() -> aiReviewService.latestReview("GlossaryTerm", goal.getId()));
		assertThrows(NoSuchEntityException.class,
				() -> aiReviewService.latestReview("Goal", 987654321L));
	}

	// ---- helpers ---------------------------------------------------------------------

	private Goal createGoal(String name) throws Exception {
		long ts = System.nanoTime();
		User creator = getUserRepository().findUserByUsername("project");
		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(creator);
		projectCommand.setName("Review Read " + ts);
		projectCommand.setOrganizationName("Review Read Org " + ts);
		projectCommand = getCommandHandler().execute(projectCommand);
		Project project = projectCommand.getProject();

		EditGoalCommand goalCommand = getProjectCommandFactory().newEditGoalCommand();
		goalCommand.setEditedBy(creator);
		goalCommand.setGoalContainer(project);
		goalCommand.setName(name + " " + ts);
		goalCommand.setText("Members get tools fast.");
		goalCommand = getCommandHandler().execute(goalCommand);
		return goalCommand.getGoal();
	}

	private static Map<String, Object> finding(String type, String severity, String issueText,
			String noteText) {
		Map<String, Object> finding = new java.util.LinkedHashMap<String, Object>();
		finding.put("findingType", type);
		finding.put("severity", severity);
		finding.put("confidence", 0.7);
		finding.put("evidenceReferences", List.of());
		finding.put("suggestedIssueText", issueText);
		finding.put("suggestedNoteText", noteText);
		finding.put("suggestedPositions", List.of());
		return finding;
	}

	@SafeVarargs
	private void reply(String summary, Map<String, Object>... findings) throws IOException {
		writeReply(objectMapper.writeValueAsString(
				Map.of("summary", summary, "findings", List.of(findings))));
	}

	private void writeReply(String modelText) throws IOException {
		Files.writeString(FAKE_CLI_DIR.resolve("reply.json"), objectMapper.writeValueAsString(
				Map.of("type", "result", "is_error", false, "result", modelText)));
	}

	private void asProjectUser() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("project", null, List.of()));
	}

	private void review(Goal goal) {
		asProjectUser();
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
			// A failed run is recorded on the row; the read is what this test checks.
		}
	}

	private RunView latest(Goal goal) {
		asProjectUser();
		return aiReviewService.latestReview("Goal", goal.getId())
				.orElseThrow(() -> new AssertionError("no review run read back"));
	}

	private static Path createFakeCli() {
		try {
			Path dir = Files.createTempDirectory("requel-fake-cli-355-");
			dir.toFile().deleteOnExit();
			Path script = dir.resolve("fake-claude");
			Files.writeString(script, String.join("\n",
					"#!/bin/sh",
					"# Fake claude for AiReviewReadIT: ignore the prompt, print the reply.",
					"here=$(dirname \"$0\")",
					"cat > /dev/null",
					"cat \"$here/reply.json\"",
					""));
			Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
			return dir;
		} catch (IOException e) {
			throw new IllegalStateException("could not create the fake CLI", e);
		}
	}
}
