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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunRepository;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;

/**
 * #262 at the client boundary: what actually reaches a remote provider. The {@code cli} provider
 * runs a fake CLI script that saves its stdin (the whole prompt) beside itself, so the test reads
 * exactly what a real {@code claude} would have been sent.
 */
@EnabledOnOs({ OS.LINUX, OS.MAC })
@TestPropertySource(properties = {
		"requel.ai.enabled=true",
		"requel.ai.provider=cli",
		"requel.ai.model=claude-cli",
		"spring.ai.model.chat=none",
		"requel.ai.cli.output-format=claude-json",
		"requel.ai.cli.timeout=30s" })
public class RedactionEgressIT extends AbstractIntegrationTestCase {

	private static final Path FAKE_CLI_DIR = createFakeCli();
	private static final String KEY = "sk-proj-AbCdEfGhIjKlMnOpQrStUvWx0123";

	@DynamicPropertySource
	static void cliCommand(DynamicPropertyRegistry registry) {
		registry.add("requel.ai.cli.command", () -> FAKE_CLI_DIR.resolve("fake-claude").toString());
	}

	private AiReviewService aiReviewService;
	private AssistantRunWorker assistantRunWorker;
	private AssistantRunRepository assistantRunRepository;
	private ProjectAssistantSettingsStore settingsStore;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	protected void setDependencies(AiReviewService aiReviewService,
			AssistantRunWorker assistantRunWorker, AssistantRunRepository assistantRunRepository,
			ProjectAssistantSettingsStore settingsStore) {
		this.aiReviewService = aiReviewService;
		this.assistantRunWorker = assistantRunWorker;
		this.assistantRunRepository = assistantRunRepository;
		this.settingsStore = settingsStore;
	}

	@AfterEach
	public void reset() throws IOException {
		SecurityContextHolder.clearContext();
		Files.deleteIfExists(FAKE_CLI_DIR.resolve("stdin.txt"));
	}

	@Test
	public void secretsEmailsAndUsernamesNeverReachTheProvider() throws Exception {
		long ts = System.currentTimeMillis();
		Goal goal = createGoal(ts, "Notify owner-" + ts + "@example.com",
				"Call the API with " + KEY + " and mail ops-" + ts + "@example.com on failure.");
		addNote(goal, "project", "Human context from the project owner");

		AssistantRunEntity run = review(goal);

		assertEquals("SUCCEEDED", run.getStatus(), "run summary: " + run.getErrorSummary());
		String sent = Files.readString(FAKE_CLI_DIR.resolve("stdin.txt"));
		assertFalse(sent.contains(KEY), "the API key reached the provider");
		assertFalse(sent.contains("owner-" + ts + "@example.com"), "the name's email leaked");
		assertFalse(sent.contains("ops-" + ts + "@example.com"), "the text's email leaked");
		assertTrue(sent.contains("[REDACTED:CREDENTIALS]"), sent);
		assertTrue(sent.contains("[REDACTED:EMAIL]"), sent);
		assertFalse(sent.contains("\"createdByUsername\" : \"project\""),
				"a username reached the provider");
		assertTrue(sent.contains("\"createdByUsername\" : \"user-1\""), sent);
		assertTrue(sent.contains("\"externalProviderAllowed\" : true"), sent);
		assertEquals(3, run.getRedactionCount());
		assertEquals(java.util.Set.of("CREDENTIALS", "EMAIL"),
				java.util.Set.of(run.getRedactionCategories().split(",")));
	}

	@Test
	public void aProjectThatDisallowsExternalProvidersFailsClosedAndSendsNothing() throws Exception {
		long ts = System.currentTimeMillis();
		// #355: base 36, so the name holds no digit run - a 13-digit millisecond time is
		// Luhn-valid one time in ten and was masked as a card, making the count flaky.
		Goal goal = createGoal(ts, "Egress off " + Long.toString(ts, 36),
				"Uses " + KEY + " somewhere.");
		settingsStore.setEnabled(goal.getProjectOrDomain().getId(), "egress.external", false,
				null);

		AssistantRunEntity run = review(goal);

		assertEquals("FAILED", run.getStatus());
		assertTrue(run.getErrorSummary().contains("does not allow sending its text"),
				run.getErrorSummary());
		assertFalse(Files.exists(FAKE_CLI_DIR.resolve("stdin.txt")), "the CLI must not run");
		assertEquals(1, run.getRedactionCount(), "what would have been masked is still recorded");
	}

	@Test
	public void aCategorySwitchedOffIsNotMaskedButTheOthersAre() throws Exception {
		long ts = System.currentTimeMillis();
		String email = "keep-" + ts + "@example.com";
		Goal goal = createGoal(ts, "Email kept " + ts, "Mail " + email + " using " + KEY + ".");
		settingsStore.setEnabled(goal.getProjectOrDomain().getId(), "redaction.email", false,
				null);

		AssistantRunEntity run = review(goal);

		assertEquals("SUCCEEDED", run.getStatus(), "run summary: " + run.getErrorSummary());
		String sent = Files.readString(FAKE_CLI_DIR.resolve("stdin.txt"));
		assertTrue(sent.contains(email), "email redaction is off for this project");
		assertFalse(sent.contains(KEY), "credentials are still masked");
	}

	// ---- helpers ---------------------------------------------------------------------

	private Goal createGoal(long ts, String name, String text) throws Exception {
		User creator = getUserRepository().findUserByUsername("project");
		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(creator);
		projectCommand.setName("Redaction Project " + ts);
		projectCommand.setOrganizationName("Redaction Org " + ts);
		projectCommand = getCommandHandler().execute(projectCommand);
		Project project = projectCommand.getProject();

		EditGoalCommand goalCommand = getProjectCommandFactory().newEditGoalCommand();
		goalCommand.setEditedBy(creator);
		goalCommand.setGoalContainer(project);
		goalCommand.setName(name);
		goalCommand.setText(text);
		goalCommand = getCommandHandler().execute(goalCommand);
		return goalCommand.getGoal();
	}

	private void addNote(Goal goal, String username, String text) throws Exception {
		EditNoteCommand cmd = getAnnotationCommandFactory().newEditNoteCommand();
		cmd.setEditedBy(getUserRepository().findUserByUsername(username));
		cmd.setGroupingObject(goal.getProjectOrDomain());
		cmd.setAnnotatable(goal);
		cmd.setText(text);
		getCommandHandler().execute(cmd);
	}

	private AssistantRunEntity review(Goal goal) throws IOException {
		Files.writeString(FAKE_CLI_DIR.resolve("reply.json"), objectMapper.writeValueAsString(
				Map.of("type", "result", "is_error", false, "result",
						"{\"summary\":\"ok\",\"findings\":[]}")));
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
		assistantRunWorker.run(queued.getRunId());
		return assistantRunRepository.findById(queued.getId())
				.orElseThrow(() -> new AssertionError("review run row vanished"));
	}

	private static Path createFakeCli() {
		try {
			Path dir = Files.createTempDirectory("requel-fake-cli-262-");
			dir.toFile().deleteOnExit();
			Path script = dir.resolve("fake-claude");
			Files.writeString(script, String.join("\n",
					"#!/bin/sh",
					"# Fake claude for RedactionEgressIT: keep the prompt, then reply.",
					"here=$(dirname \"$0\")",
					"cat > \"$here/stdin.txt\"",
					"cat \"$here/reply.json\"",
					""));
			Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
			return dir;
		} catch (IOException e) {
			throw new IllegalStateException("could not create the fake CLI", e);
		}
	}
}
