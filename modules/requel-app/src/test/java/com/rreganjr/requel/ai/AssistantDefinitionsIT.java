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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.assistant.AbstractLexicalAssistantTest;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinitionStore;
import com.rreganjr.requel.assistant.core.definition.DefinitionSource;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.DeleteProjectCommand;
import com.rreganjr.requel.project.command.EditProjectAssistantSettingCommand;

/**
 * Issue #260 end to end: the bundled requirements-review definition is seeded, runs through the
 * generic executor against a fake CLI (as {@link CliProviderReviewIT}), and can be forked per
 * project (and go with the project when it is deleted) and switched off install-wide or per
 * project. The review writes as its own
 * {@code assistant-<id>} user.
 */
@EnabledOnOs({ OS.LINUX, OS.MAC })
@TestPropertySource(properties = {
		"requel.ai.enabled=true",
		"requel.ai.provider=cli",
		"requel.ai.model=claude-cli",
		"spring.ai.model.chat=none",
		"requel.ai.cli.output-format=claude-json",
		"requel.ai.cli.timeout=30s" })
public class AssistantDefinitionsIT extends AbstractLexicalAssistantTest {

	/** The definition that reviews goals (#263; before that, the generic fallback did). */
	private static final String REVIEW = "ai-review-goal";
	private static final Path FAKE_CLI_DIR = createFakeCli();

	@DynamicPropertySource
	static void cliCommand(DynamicPropertyRegistry registry) {
		registry.add("requel.ai.cli.command", () -> FAKE_CLI_DIR.resolve("fake-claude").toString());
	}

	private AiReviewService aiReviewService;
	private AssistantDefinitionStore definitionStore;
	private JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	protected void setDependencies(AiReviewService aiReviewService,
			AssistantDefinitionStore definitionStore, JdbcTemplate jdbcTemplate) {
		this.aiReviewService = aiReviewService;
		this.definitionStore = definitionStore;
		this.jdbcTemplate = jdbcTemplate;
	}

	@AfterEach
	public void reset() throws IOException {
		SecurityContextHolder.clearContext();
		Files.deleteIfExists(FAKE_CLI_DIR.resolve("reply.json"));
		Files.deleteIfExists(FAKE_CLI_DIR.resolve("last-prompt"));
	}

	@Test
	public void theBundledReviewDefinitionIsSeeded() {
		AssistantDefinition review = bundledReview();
		assertEquals(DefinitionSource.BUNDLED, review.source());
		assertEquals(1, review.version());
		assertTrue(review.enabled());
		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM assistant_definitions WHERE definition_key = ?"
						+ " AND project_id IS NULL", Integer.class, REVIEW));
	}

	@Test
	public void aForkedDefinitionChangesOnlyItsProjectsPrompt() throws Exception {
		Project plain = newProject("DefPlain");
		Project forked = newProject("DefForked");
		String marker = "Also flag every mention of penguins " + stamp() + ".";
		definitionStore.save(bundledReview().forkFor(forked.getId(),
				bundledReview().instructions() + "\n" + marker), "project");
		reply(List.of());

		AssistantRunEntity plainRun = review(newGoal(plain, projectUser(), "Fast search " + stamp(),
				"Search results should be fast."));
		String plainPrompt = lastPrompt();
		AssistantRunEntity forkedRun = review(newGoal(forked, projectUser(),
				"Fast search " + stamp(), "Search results should be fast."));
		String forkedPrompt = lastPrompt();

		assertEquals("SUCCEEDED", plainRun.getStatus(), plainRun.getErrorSummary());
		assertEquals("SUCCEEDED", forkedRun.getStatus(), forkedRun.getErrorSummary());
		assertFalse(plainPrompt.contains(marker), "the other project keeps the bundled prompt");
		assertTrue(forkedPrompt.contains(marker), "the forked project's prompt has its change");
		assertEquals("BUNDLED", plainRun.getTemplateSource());
		assertEquals("PROJECT", forkedRun.getTemplateSource());
	}

	@Test
	public void deletingAProjectDeletesItsDefinitions() throws Exception {
		Project project = newProject("DefDelete");
		definitionStore.save(bundledReview().forkFor(project.getId(), "A project prompt."),
				"project");
		assertEquals(1, ownedDefinitions(project));

		DeleteProjectCommand delete = getProjectCommandFactory().newDeleteProjectCommand();
		delete.setEditedBy(projectUser());
		delete.setProject(getProjectRepository().get(project));
		getCommandHandler().execute(delete);

		assertEquals(0, ownedDefinitions(project));
		assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assistant_definitions"
				+ " WHERE definition_key = ? AND project_id IS NULL", Integer.class, REVIEW),
				"the bundled definition stays");
	}

	@Test
	public void switchingTheDefinitionOffInstallWideSkipsTheNextRun() throws Exception {
		Project project = newProject("DefOff");
		reply(List.of());
		AssistantDefinition review = bundledReview();
		try {
			definitionStore.save(review.withEnabled(false), "admin");
			AssistantRunEntity off = review(newGoal(project, projectUser(), "Fast " + stamp(),
					"Search results should be fast."));
			assertEquals("SKIPPED", off.getStatus(), "disabled install-wide: nothing runs");
		} finally {
			definitionStore.save(review.withEnabled(true), "admin");
		}
		AssistantRunEntity on = review(newGoal(project, projectUser(), "Fast " + stamp(),
				"Search results should be fast."));
		assertEquals("SUCCEEDED", on.getStatus(), "enabled again, the next run runs");
	}

	@Test
	public void switchingTheDefinitionOffForAProjectSkipsOnlyThatProject() throws Exception {
		Project off = newProject("DefProjectOff");
		Project on = newProject("DefProjectOn");
		reply(List.of());
		setEnabled(off, false);

		assertEquals("SKIPPED", review(newGoal(off, projectUser(), "Fast " + stamp(),
				"Search results should be fast.")).getStatus());
		assertEquals("SUCCEEDED", review(newGoal(on, projectUser(), "Fast " + stamp(),
				"Search results should be fast.")).getStatus());

		setEnabled(off, true);
		assertEquals("SUCCEEDED", review(newGoal(off, projectUser(), "Fast " + stamp(),
				"Search results should be fast.")).getStatus(), "switched back on");
	}

	/**
	 * The review writes as {@code assistant-ai-requirements-review}, not the legacy
	 * {@code assistant}; {@code ProjectAssistantSettingsIT} checks the lexical side (it already
	 * loads the dictionary, which costs a context its own copy).
	 */
	@Test
	public void theReviewWritesAsItsOwnMachineIdentity() throws Exception {
		Project project = newProject("DefAuthor");
		long ts = System.currentTimeMillis();
		reply(List.of(finding("'fast' is not measurable (" + ts + ")", List.of("fast"))));

		AssistantRunEntity run = review(newGoal(project, projectUser(), "Fast search " + stamp(),
				"Search results should be fast."));

		assertEquals("SUCCEEDED", run.getStatus(), run.getErrorSummary());
		String author = authorOf("%not measurable (" + ts + ")%");
		assertEquals("assistant-" + REVIEW, author);
		assertTrue(User.isAssistant(getUserRepository().findUserByUsername(author)));
		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM stakeholders s JOIN users u ON s.user_id = u.id"
						+ " WHERE u.username = ? AND s.projectordomain_id = ?", Integer.class,
				author, project.getId()),
				"the identity became a stakeholder of the project on its first write");
	}

	@Test
	public void theRunRecordsItsDefinitionAndUnverifiedEvidence() throws Exception {
		Project project = newProject("DefRecord");
		long ts = System.currentTimeMillis();
		reply(List.of(
				finding("'fast' is not measurable (" + ts + ")", List.of("fast")),
				finding("warp drive is undefined (" + ts + ")", List.of("warp drive"))));

		AssistantRunEntity run = review(newGoal(project, projectUser(), "Fast search " + stamp(),
				"Search results should be fast."));

		assertEquals("SUCCEEDED", run.getStatus(), run.getErrorSummary());
		assertEquals(REVIEW, run.getTemplateId());
		assertEquals("1", run.getTemplateVersion());
		assertEquals("BUNDLED", run.getTemplateSource());
		assertEquals(1, run.getEvidenceUnverified(),
				"'warp drive' is not in the goal; 'fast' is");
		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM annotations WHERE text LIKE ?", Integer.class,
				"%warp drive is undefined (" + ts + ")%"),
				"an unverified finding is still written (the check warns, it doesn't drop)");
	}

	// ---- helpers ---------------------------------------------------------------------

	private AssistantDefinition bundledReview() {
		return definitionStore.bundled().stream().filter(d -> REVIEW.equals(d.key())).findFirst()
				.orElseThrow(() -> new AssertionError("no bundled " + REVIEW));
	}

	private int ownedDefinitions(Project project) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM assistant_definitions WHERE project_id = ?", Integer.class,
				project.getId());
	}

	private void setEnabled(Project project, boolean enabled) throws Exception {
		EditProjectAssistantSettingCommand command = getProjectCommandFactory()
				.newEditProjectAssistantSettingCommand();
		command.setEditedBy(projectUser());
		command.setProject(project);
		command.setAssistantId(REVIEW);
		command.setEnabled(enabled);
		getCommandHandler().execute(command);
	}

	private String authorOf(String textPattern) {
		List<String> authors = jdbcTemplate.queryForList(
				"SELECT u.username FROM annotations a JOIN users u ON a.created_by_id = u.id"
						+ " WHERE a.text LIKE ?", String.class, textPattern);
		assertFalse(authors.isEmpty(), "no annotation matching " + textPattern);
		return authors.get(0);
	}

	private static Map<String, Object> finding(String issueText, List<String> evidence) {
		return Map.of(
				"findingType", "UNTESTABLE",
				"severity", "HIGH",
				"confidence", 0.8,
				"evidenceReferences", evidence,
				"suggestedIssueText", issueText,
				"suggestedPositions", List.of("Rewrite it measurably"));
	}

	private void reply(List<Map<String, Object>> findings) throws IOException {
		String reply = objectMapper.writeValueAsString(Map.of(
				"summary", findings.size() + " findings",
				"findings", findings,
				"warnings", List.of()));
		Files.writeString(FAKE_CLI_DIR.resolve("reply.json"), objectMapper.writeValueAsString(
				Map.of("type", "result", "is_error", false, "result", reply,
						"total_cost_usd", 0.001,
						"usage", Map.of("input_tokens", 100, "output_tokens", 20))));
	}

	private String lastPrompt() throws IOException {
		return Files.readString(FAKE_CLI_DIR.resolve("last-prompt"));
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
		assistantRunWorker.run(queued.getRunId());
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
					"# Fake claude for AssistantDefinitionsIT: keep the prompt, then reply.",
					"here=$(dirname \"$0\")",
					"cat > \"$here/last-prompt\"",
					"cat \"$here/reply.json\"",
					""));
			Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
			return dir;
		} catch (IOException e) {
			throw new IllegalStateException("could not create the fake CLI", e);
		}
	}
}
