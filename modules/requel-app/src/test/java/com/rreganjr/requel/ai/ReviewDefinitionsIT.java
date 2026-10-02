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
import java.util.HashMap;
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
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.assistant.AbstractLexicalAssistantTest;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinitionStore;
import com.rreganjr.requel.assistant.core.definition.DefinitionSource;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditProjectAssistantSettingCommand;

/**
 * Issue #263 end to end, with the fake CLI: the per-type definitions run (GlossaryTerm included),
 * re-runs don't pile up issues (#360), a per-type definition retires the fallback's findings,
 * extraction findings are advisory with a one-click position, and off-vocabulary types are
 * counted on the run.
 */
@EnabledOnOs({ OS.LINUX, OS.MAC })
@TestPropertySource(properties = {
		"requel.ai.enabled=true",
		"requel.ai.provider=cli",
		"requel.ai.model=claude-cli",
		"spring.ai.model.chat=none",
		"requel.ai.cli.output-format=claude-json",
		"requel.ai.cli.timeout=30s" })
public class ReviewDefinitionsIT extends AbstractLexicalAssistantTest {

	private static final String FALLBACK = "ai-requirements-review";
	private static final String GOAL = "ai-review-goal";
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
		Files.deleteIfExists(FAKE_CLI_DIR.resolve("last-prompt"));
	}

	@Test
	public void aGoalIsReviewedByTheGoalDefinitionWithItsContext() throws Exception {
		Goal goal = newGoal(newProject("DefGoal"), projectUser(), "Fast search " + stamp(),
				"Search results should be fast.");
		reply(List.of());

		AssistantRunEntity run = review("Goal", goal);

		assertEquals("SUCCEEDED", run.getStatus(), run.getErrorSummary());
		assertEquals(GOAL, run.getTemplateId());
		String prompt = lastPrompt();
		assertTrue(prompt.contains("SOLUTION_NOT_OUTCOME"), "the vocabulary reaches the prompt");
		assertFalse(prompt.contains("{{vocabulary}}"));
	}

	@Test
	public void aGlossaryTermCanBeReviewed() throws Exception {
		Project project = newProject("DefTerm");
		GlossaryTerm loan = term(project, "Loan " + stamp(), "A loan is when a tool is loaned.");
		term(project, "Overdue loan " + stamp(), "A loan past its return date.");
		reply(List.of(finding("CIRCULAR_DEFINITION", "The definition uses the term itself.",
				List.of("loaned"), null)));

		AssistantRunEntity run = review("GlossaryTerm", loan);

		assertEquals("SUCCEEDED", run.getStatus(), run.getErrorSummary());
		assertEquals("ai-review-glossary-term", run.getTemplateId());
		assertTrue(lastPrompt().contains("glossary-related"), "the glossary provider ran");
		assertEquals(1, aiIssues("GlossaryTerm", loan, "ai-review-glossary-term"));
	}

	@Test
	public void aReRunRemovesTheUntouchedIssueItNoLongerReportsAndKeepsADiscussedOne()
			throws Exception {
		Goal goal = newGoal(newProject("DefRerun"), projectUser(), "Reminders " + stamp(),
				"Send overdue reminders by text message.");
		long ts = System.currentTimeMillis();
		reply(List.of(
				finding("SOLUTION_NOT_OUTCOME", "Untouched finding " + ts, List.of("text"), null),
				finding("AMBIGUOUS", "Discussed finding " + ts, List.of("reminders"), null)));
		review("Goal", goal);
		assertEquals(2, aiIssues("Goal", goal, GOAL));
		Issue discussed = issueWithText("Discussed finding " + ts);
		addPosition(discussed, projectUser(), "We should say how often.");

		reply(List.of());
		review("Goal", goal);

		assertEquals(0, count("SELECT COUNT(*) FROM annotations WHERE text = ?",
				"Untouched finding " + ts), "an untouched issue the re-run dropped is removed");
		assertEquals(1, count("SELECT COUNT(*) FROM annotations WHERE text = ?",
				"Discussed finding " + ts), "a discussed one is kept");
	}

	@Test
	public void theGoalDefinitionRetiresTheFallbacksFindingsOnTheGoal() throws Exception {
		Project project = newProject("DefRetire");
		Goal goal = newGoal(project, projectUser(), "Reminders " + stamp(),
				"Send overdue reminders by text message.");
		long ts = System.currentTimeMillis();
		// As before #263: the project's copy of the goal definition serves another task, so no
		// definition covers goals and the fallback reviews this one.
		AssistantDefinition bundled = definitionStore.bundled().stream()
				.filter(d -> GOAL.equals(d.key())).findFirst().orElseThrow();
		definitionStore.save(new AssistantDefinition(GOAL, bundled.displayName(), bundled.kind(),
				"SOME_OTHER_TASK", bundled.scope(), bundled.contextProviders(),
				bundled.instructions(), bundled.vocabulary(), bundled.outputSchemaName(),
				bundled.outputSchemaVersion(), true, 1, DefinitionSource.PROJECT, project.getId(),
				bundled.version(), null), "project");
		reply(List.of(finding("AMBIGUOUS", "Fallback finding " + ts, List.of("text"), null)));
		AssistantRunEntity first = review("Goal", goal);
		assertEquals(FALLBACK, first.getTemplateId());
		assertEquals(1, aiIssues("Goal", goal, FALLBACK));

		definitionStore.deleteForProject(project.getId());
		reply(List.of());
		AssistantRunEntity second = review("Goal", goal);

		assertEquals(GOAL, second.getTemplateId());
		assertEquals(0, count("SELECT COUNT(*) FROM annotations WHERE text = ?",
				"Fallback finding " + ts), "the per-type definition retired the fallback's issue");
	}

	@Test
	public void switchingTheGoalDefinitionOffStopsGoalReviews() throws Exception {
		Project project = newProject("DefOff");
		Goal goal = newGoal(project, projectUser(), "Search " + stamp(), "Search is fast.");
		setEnabled(project, GOAL, false);
		reply(List.of());

		assertEquals("SKIPPED", review("Goal", goal).getStatus(),
				"the fallback does not take over a type with its own definition");
	}

	@Test
	public void anExtractedActorIsAdvisoryWithAOneClickPosition() throws Exception {
		Goal goal = newGoal(newProject("DefExtract"), projectUser(), "Approvals " + stamp(),
				"Dana approves tool purchases.");
		String role = "Treasurer" + stamp();
		reply(List.of(finding("EXTRACT_ACTOR", "Dana stands for a role the project has no actor for.",
				List.of("Dana"), role)));

		review("Goal", goal);

		Map<String, Object> issue = jdbcTemplate.queryForMap("SELECT a.id, a.annotation_type,"
				+ " a.must_be_resolved, a.severity, a.word FROM annotations a"
				+ " WHERE a.text = ?", "Dana stands for a role the project has no actor for.");
		assertEquals("com.rreganjr.requel.annotation.impl.LexicalIssue", issue.get("annotation_type"));
		assertEquals(role, issue.get("word"));
		assertEquals(Boolean.FALSE, issue.get("must_be_resolved"));
		assertEquals("LOW", issue.get("severity"));
		assertEquals(1, count("SELECT COUNT(*) FROM positions p JOIN position_issue pi"
				+ " ON pi.position_id = p.id WHERE pi.issue_id = ? AND p.position_type ="
				+ " 'com.rreganjr.requel.project.impl.AddActorPosition'", issue.get("id")),
				"the add-actor position is offered");
	}

	@Test
	public void offVocabularyTypesAreCountedOnTheRun() throws Exception {
		Goal goal = newGoal(newProject("DefMiss"), projectUser(), "Search " + stamp(),
				"Search results should be fast.");
		reply(List.of(finding("UNTESTABLE", "Not testable " + System.nanoTime(), List.of("fast"),
				null)));

		AssistantRunEntity run = review("Goal", goal);

		assertEquals(1, run.getVocabularyMisses(), "UNTESTABLE is not in the goal vocabulary");
	}

	// ---- helpers -------------------------------------------------------------------------

	private GlossaryTerm term(Project project, String name, String text) throws Exception {
		EditGlossaryTermCommand command = getProjectCommandFactory().newEditGlossaryTermCommand();
		command.setEditedBy(projectUser());
		command.setProjectOrDomain(project);
		command.setName(name);
		command.setText(text);
		return getCommandHandler().execute(command).getGlossaryTerm();
	}

	private void setEnabled(Project project, String assistantId, boolean enabled) throws Exception {
		EditProjectAssistantSettingCommand command = getProjectCommandFactory()
				.newEditProjectAssistantSettingCommand();
		command.setEditedBy(projectUser());
		command.setProject(project);
		command.setAssistantId(assistantId);
		command.setEnabled(enabled);
		getCommandHandler().execute(command);
	}

	private int aiIssues(String type, ProjectOrDomainEntity entity, String key) {
		return count("SELECT COUNT(*) FROM annotations a JOIN annotation_annotatable aa"
				+ " ON aa.annotation_id = a.id WHERE aa.annotatable_type = ? AND aa.annotatable_id = ?"
				+ " AND a.assistant_idempotency_key LIKE ?", type, entity.getId(), key + ":%");
	}

	private Issue issueWithText(String text) {
		Long id = jdbcTemplate.queryForObject("SELECT id FROM annotations WHERE text = ?",
				Long.class, text);
		return (Issue) getAnnotationRepository().findAnnotationById(id);
	}

	private int count(String sql, Object... args) {
		return jdbcTemplate.queryForObject(sql, Integer.class, args);
	}

	private static Map<String, Object> finding(String type, String issueText,
			List<String> evidence, String entityName) {
		Map<String, Object> finding = new HashMap<>();
		finding.put("findingType", type);
		finding.put("severity", "MEDIUM");
		finding.put("confidence", 0.8);
		finding.put("evidenceReferences", evidence);
		finding.put("suggestedIssueText", issueText);
		finding.put("suggestedNoteText", null);
		finding.put("suggestedPositions", List.of());
		finding.put("suggestedEntityName", entityName);
		return finding;
	}

	private void reply(List<Map<String, Object>> findings) throws IOException {
		String reply = objectMapper.writeValueAsString(Map.of("summary", findings.size()
				+ " findings", "findings", findings, "warnings", List.of()));
		Files.writeString(FAKE_CLI_DIR.resolve("reply.json"), objectMapper.writeValueAsString(
				Map.of("type", "result", "is_error", false, "result", reply,
						"total_cost_usd", 0.001,
						"usage", Map.of("input_tokens", 100, "output_tokens", 20))));
	}

	private String lastPrompt() throws IOException {
		return Files.readString(FAKE_CLI_DIR.resolve("last-prompt"));
	}

	private AssistantRunEntity review(String type, ProjectOrDomainEntity entity) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("project", null, List.of()));
		aiReviewService.requestReview(type, entity.getId());
		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(run -> entity.getId().equals(run.getTargetId())
						&& "REQUIREMENTS_REVIEW".equals(run.getTaskType())
						&& "QUEUED".equals(run.getStatus()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no QUEUED review run for " + type));
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
					"# Fake claude for ReviewDefinitionsIT: keep the prompt, then reply.",
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
