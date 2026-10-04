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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.rreganjr.requel.assistant.AbstractLexicalAssistantTest;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantDefinitions;
import com.rreganjr.requel.project.command.AssistantDefinitionCommand;
import com.rreganjr.requel.service.command.AnnotationSources;

/**
 * Issue #264 end to end, with the fake CLI: a project's copy of a bundled definition changes that
 * project's review and no other's, and the run records it as the project's; a project's own policy
 * runs and its issues read under its name; a definition that leads the model into malformed
 * output fails the run with no annotations.
 */
@EnabledOnOs({ OS.LINUX, OS.MAC })
@TestPropertySource(properties = {
		"requel.ai.enabled=true",
		"requel.ai.provider=cli",
		"requel.ai.model=claude-cli",
		"spring.ai.model.chat=none",
		"requel.ai.cli.output-format=claude-json",
		"requel.ai.cli.timeout=30s" })
public class ProjectDefinitionsIT extends AbstractLexicalAssistantTest {

	private static final String GOAL_REVIEW = "ai-review-goal";
	private static final String MARKER = "HOUSE-RULE-264";
	private static final Path FAKE_CLI_DIR = createFakeCli();

	@DynamicPropertySource
	static void cliCommand(DynamicPropertyRegistry registry) {
		registry.add("requel.ai.cli.command", () -> FAKE_CLI_DIR.resolve("fake-claude").toString());
	}

	private AiReviewService aiReviewService;
	private JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	protected void setDependencies(AiReviewService aiReviewService, JdbcTemplate jdbcTemplate) {
		this.aiReviewService = aiReviewService;
		this.jdbcTemplate = jdbcTemplate;
	}

	@AfterEach
	public void reset() throws IOException {
		SecurityContextHolder.clearContext();
		Files.deleteIfExists(FAKE_CLI_DIR.resolve("last-review-prompt"));
	}

	@Test
	public void aForkChangesOnlyItsProjectsReviewAndTheRunSaysSo() throws Exception {
		Project ours = newProject("DefOurs");
		Project theirs = newProject("DefTheirs");
		Goal ourGoal = goal(ours);
		Goal theirGoal = goal(theirs);
		ProjectAssistantDefinitions.View fork = fork(ours, GOAL_REVIEW);
		edit(ours, fork, "Goals name a measurable outcome. " + MARKER);
		reply("reply.json", List.of());

		Map<String, AssistantRunEntity> ourRuns = requestAndRun(ourGoal);
		String ourPrompt = Files.readString(FAKE_CLI_DIR.resolve("last-review-prompt"));
		requestAndRun(theirGoal);
		String theirPrompt = Files.readString(FAKE_CLI_DIR.resolve("last-review-prompt"));

		assertTrue(ourPrompt.contains(MARKER), "the project's instructions are sent");
		assertFalse(theirPrompt.contains(MARKER), "another project still runs the bundled one");
		AssistantRunEntity run = ourRuns.get(AiReviewService.TASK_TYPE);
		assertEquals("SUCCEEDED", run.getStatus(), run.getErrorSummary());
		assertTrue(run.getTemplateId().contains(GOAL_REVIEW));
		assertTrue(run.getTemplateSource().contains("PROJECT"), run.getTemplateSource());
		assertTrue(aiReviewService.latestReview("Goal", ourGoal.getId()).orElseThrow()
				.definitionSources().contains("PROJECT"));
	}

	@Test
	public void aProjectsOwnPolicyRunsAndItsIssuesReadUnderItsName() throws Exception {
		Project project = newProject("DefPolicy");
		Goal goal = goal(project);
		create(project, "house-style", "Every goal names its owner.");
		reply("reply.json", List.of());
		reply("policy-reply.json", List.of(finding("house-style", "RULE_BROKEN",
				"The goal names no owner.")));

		Map<String, AssistantRunEntity> runs = requestAndRun(goal);

		AssistantRunEntity policy = runs.get(AiReviewService.POLICY_TASK_TYPE);
		assertEquals("SUCCEEDED", policy.getStatus(), policy.getErrorSummary());
		assertEquals(1, aiIssues(goal, "house-style"));
		assertEquals(AnnotationSources.POLICY,
				AnnotationSources.kind("ASSISTANT:house-style", project.getId()));
		assertEquals("House style", AnnotationSources.name("ASSISTANT:house-style",
				project.getId()));
		assertNull(AnnotationSources.kind("ASSISTANT:house-style"),
				"another project doesn't know it");
	}

	@Test
	public void malformedOutputFailsTheRunWithNoAnnotations() throws Exception {
		Project project = newProject("DefBroken");
		Goal goal = goal(project);
		ProjectAssistantDefinitions.View fork = fork(project, GOAL_REVIEW);
		edit(project, fork, "Answer in prose, not JSON.");
		Files.writeString(FAKE_CLI_DIR.resolve("reply.json"), objectMapper.writeValueAsString(
				Map.of("type", "result", "is_error", false, "result",
						"Here are my thoughts about the goal.", "total_cost_usd", 0.001,
						"usage", Map.of("input_tokens", 100, "output_tokens", 20))));

		AssistantRunEntity run = requestAndRun(goal).get(AiReviewService.TASK_TYPE);

		assertEquals("FAILED", run.getStatus());
		assertEquals(0, aiIssues(goal, GOAL_REVIEW));
	}

	private Goal goal(Project project) throws Exception {
		return newGoal(project, projectUser(), "Renew online " + stamp(),
				"A member can renew a loan online.");
	}

	private ProjectAssistantDefinitions.View fork(Project project, String key) throws Exception {
		AssistantDefinitionCommand command = getProjectCommandFactory()
				.newForkAssistantDefinitionCommand();
		command.setEditedBy(projectUser());
		command.setProject(project);
		command.setKey(key);
		return getCommandHandler().execute(command).getDefinition();
	}

	private void edit(Project project, ProjectAssistantDefinitions.View view, String instructions)
			throws Exception {
		AssistantDefinitionCommand command = getProjectCommandFactory()
				.newEditAssistantDefinitionCommand();
		command.setEditedBy(projectUser());
		command.setProject(project);
		command.setKey(view.key());
		command.setLockVersion(view.lockVersion());
		command.setDraft(new ProjectAssistantDefinitions.Draft(null, view.key(),
				view.displayName(), view.scope(), view.contextProviders(), view.contextBudgets(),
				instructions, view.vocabulary(), view.localOnly(), null));
		getCommandHandler().execute(command);
	}

	private void create(Project project, String key, String rule) throws Exception {
		AssistantDefinitionCommand command = getProjectCommandFactory()
				.newCreateAssistantDefinitionCommand();
		command.setEditedBy(projectUser());
		command.setProject(project);
		command.setDraft(new ProjectAssistantDefinitions.Draft("POLICY", key, "House style",
				Set.of(), List.of("entity"), Map.of(), rule + "\n\n{{vocabulary}}",
				List.of(new ProjectAssistantDefinitions.Vocabulary("RULE_BROKEN",
						"the rule is broken", null)), false, null));
		getCommandHandler().execute(command);
	}

	/** Request a review, then run every run it queued; by task type. */
	private Map<String, AssistantRunEntity> requestAndRun(Goal goal) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("project", null, List.of()));
		aiReviewService.requestReview("Goal", goal.getId());
		Map<String, AssistantRunEntity> runs = new HashMap<>();
		for (AssistantRunEntity queued : assistantRunRepository.findAll()) {
			if (goal.getId().equals(queued.getTargetId()) && "QUEUED".equals(queued.getStatus())) {
				assistantRunWorker.run(queued.getRunId());
				runs.put(queued.getTaskType(), assistantRunRepository.findById(queued.getId())
						.orElseThrow(() -> new AssertionError("run row vanished")));
			}
		}
		return runs;
	}

	private int aiIssues(Goal goal, String key) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM annotations a JOIN"
				+ " annotation_annotatable aa ON aa.annotation_id = a.id WHERE"
				+ " aa.annotatable_type = 'Goal' AND aa.annotatable_id = ?"
				+ " AND a.assistant_idempotency_key LIKE ?", Integer.class, goal.getId(),
				key + ":%");
	}

	private static Map<String, Object> finding(String policyKey, String type, String issueText) {
		Map<String, Object> finding = new HashMap<>();
		finding.put("policyKey", policyKey);
		finding.put("findingType", type);
		finding.put("severity", "MEDIUM");
		finding.put("confidence", 0.8);
		finding.put("evidenceReferences", List.of());
		finding.put("suggestedIssueText", issueText);
		finding.put("suggestedNoteText", null);
		finding.put("suggestedPositions", List.of());
		finding.put("suggestedEntityName", null);
		return finding;
	}

	private void reply(String file, List<Map<String, Object>> findings) throws IOException {
		String reply = objectMapper.writeValueAsString(Map.of("summary", findings.size()
				+ " findings", "findings", findings, "warnings", List.of()));
		Files.writeString(FAKE_CLI_DIR.resolve(file), objectMapper.writeValueAsString(
				Map.of("type", "result", "is_error", false, "result", reply,
						"total_cost_usd", 0.001,
						"usage", Map.of("input_tokens", 100, "output_tokens", 20))));
	}

	/** Replies from policy-reply.json to the policy pass and reply.json otherwise. */
	private static Path createFakeCli() {
		try {
			Path dir = Files.createTempDirectory("requel-fake-cli-");
			dir.toFile().deleteOnExit();
			Path script = dir.resolve("fake-claude");
			Files.writeString(script, String.join("\n",
					"#!/bin/sh",
					"# Fake claude for ProjectDefinitionsIT.",
					"here=$(dirname \"$0\")",
					"cat > \"$here/prompt\"",
					"if grep -q POLICY_REVIEW \"$here/prompt\"; then",
					"  cat \"$here/policy-reply.json\"",
					"else",
					"  cp \"$here/prompt\" \"$here/last-review-prompt\"",
					"  cat \"$here/reply.json\"",
					"fi",
					""));
			Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
			return dir;
		} catch (IOException e) {
			throw new IllegalStateException("could not create the fake CLI", e);
		}
	}
}
