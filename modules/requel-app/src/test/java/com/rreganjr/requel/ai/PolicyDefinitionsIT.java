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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.rreganjr.requel.assistant.core.persistence.AssistantRunReadService;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditProjectAssistantSettingCommand;
import com.rreganjr.requel.service.command.AnnotationSources;

/**
 * Issue #265 end to end, with the fake CLI: a review request also dispatches the policy pass when
 * a policy applies; the terminology policy sees the glossary; its findings are attributed to it,
 * read as POLICY, and coexist with the review's; each pass cleans up only its own findings; a
 * switched-off policy means no policy run.
 */
@EnabledOnOs({ OS.LINUX, OS.MAC })
@TestPropertySource(properties = {
		"requel.ai.enabled=true",
		"requel.ai.provider=cli",
		"requel.ai.model=claude-cli",
		"spring.ai.model.chat=none",
		"requel.ai.cli.output-format=claude-json",
		"requel.ai.cli.timeout=30s" })
public class PolicyDefinitionsIT extends AbstractLexicalAssistantTest {

	private static final String TERMINOLOGY = "ai-policy-terminology";
	private static final String GOAL = "ai-review-goal";
	private static final String REVIEW = AiReviewService.TASK_TYPE;
	private static final String POLICY = AiReviewService.POLICY_TASK_TYPE;
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
		Files.deleteIfExists(FAKE_CLI_DIR.resolve("last-policy-prompt"));
	}

	@Test
	public void theTerminologyPolicyRunsBesideTheReviewAndItsFindingIsAttributedToIt()
			throws Exception {
		Project project = newProject("PolTerm");
		Goal goal = withPatronGlossary(project);
		reply("reply.json", List.of(finding(null, "AMBIGUOUS", "Online is unclear.")));
		reply("policy-reply.json", List.of(finding(TERMINOLOGY, "NON_CANONICAL_TERM",
				"Uses 'patron' where the glossary's term is 'Member'.")));

		Map<String, AssistantRunEntity> runs = requestAndRun(goal);

		AssistantRunEntity policy = runs.get(POLICY);
		assertEquals("SUCCEEDED", policy.getStatus(), policy.getErrorSummary());
		assertEquals(TERMINOLOGY, policy.getTemplateId());
		String prompt = Files.readString(FAKE_CLI_DIR.resolve("last-policy-prompt"));
		assertTrue(prompt.contains("## Policy " + TERMINOLOGY), "the policy's rule is sent");
		assertTrue(prompt.contains("project-glossary") && prompt.contains("Patron"),
				"the glossary, with its alternate names, is sent");
		assertEquals(1, aiIssues(goal, TERMINOLOGY));
		assertEquals(1, aiIssues(goal, GOAL));
		assertEquals(AnnotationSources.POLICY, AnnotationSources.kind("ASSISTANT:" + TERMINOLOGY));
		assertEquals("AI terminology policy", AnnotationSources.name("ASSISTANT:" + TERMINOLOGY));
		assertEquals(AnnotationSources.REVIEW, AnnotationSources.kind("ASSISTANT:" + GOAL));

		// The read takes the task type; the review stays the default.
		Optional<AssistantRunReadService.RunView> read = aiReviewService.latestReview("Goal",
				goal.getId(), POLICY);
		assertTrue(read.isPresent());
		assertEquals(1, read.get().findings().size());
		assertTrue(aiReviewService.latestReview("Goal", goal.getId()).orElseThrow()
				.definitionKeys().contains(GOAL));
		assertThrows(IllegalArgumentException.class,
				() -> aiReviewService.latestReview("Goal", goal.getId(), "SOMETHING_ELSE"));
	}

	@Test
	public void eachPassCleansUpOnlyItsOwnFindings() throws Exception {
		Goal goal = withPatronGlossary(newProject("PolClean"));
		reply("reply.json", List.of(finding(null, "AMBIGUOUS", "Online is unclear.")));
		reply("policy-reply.json", List.of(finding(TERMINOLOGY, "NON_CANONICAL_TERM",
				"Uses 'patron' where the glossary's term is 'Member'.")));
		requestAndRun(goal);

		// The review no longer reports its finding; the policy still does.
		reply("reply.json", List.of());
		requestAndRun(goal);
		assertEquals(0, aiIssues(goal, GOAL));
		assertEquals(1, aiIssues(goal, TERMINOLOGY));

		// Now the policy doesn't either.
		reply("policy-reply.json", List.of());
		requestAndRun(goal);
		assertEquals(0, aiIssues(goal, TERMINOLOGY));
	}

	@Test
	public void aSwitchedOffPolicyMeansNoPolicyRun() throws Exception {
		Project project = newProject("PolOff");
		Goal goal = withPatronGlossary(project);
		setEnabled(project, TERMINOLOGY, false);
		reply("reply.json", List.of());

		Map<String, AssistantRunEntity> runs = requestAndRun(goal);

		assertTrue(runs.containsKey(REVIEW));
		assertTrue(!runs.containsKey(POLICY), "no policy applies, so no policy run");
	}

	/** A project glossary with "Member" and its alternate "Patron", and a goal using "patron". */
	private Goal withPatronGlossary(Project project) throws Exception {
		GlossaryTerm member = term(project, "Member", "A person with a current membership.", null);
		term(project, "Patron", "See Member.", member);
		return newGoal(project, projectUser(), "Patrons renew online " + stamp(),
				"A patron can renew a loan online.");
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

	private GlossaryTerm term(Project project, String name, String text, GlossaryTerm canonical)
			throws Exception {
		EditGlossaryTermCommand command = getProjectCommandFactory().newEditGlossaryTermCommand();
		command.setEditedBy(projectUser());
		command.setProjectOrDomain(project);
		command.setName(name + " " + stamp());
		command.setText(text);
		if (canonical != null) {
			command.setCanonicalTerm(canonical);
		}
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

	private int aiIssues(ProjectOrDomainEntity entity, String key) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM annotations a JOIN"
				+ " annotation_annotatable aa ON aa.annotation_id = a.id WHERE"
				+ " aa.annotatable_type = 'Goal' AND aa.annotatable_id = ?"
				+ " AND a.assistant_idempotency_key LIKE ?", Integer.class, entity.getId(),
				key + ":%");
	}

	private static Map<String, Object> finding(String policyKey, String type, String issueText) {
		Map<String, Object> finding = new HashMap<>();
		if (policyKey != null) {
			finding.put("policyKey", policyKey);
		}
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

	/** Replies from policy-reply.json to the policy pass (its prompt names the task) and reply.json otherwise. */
	private static Path createFakeCli() {
		try {
			Path dir = Files.createTempDirectory("requel-fake-cli-");
			dir.toFile().deleteOnExit();
			Path script = dir.resolve("fake-claude");
			Files.writeString(script, String.join("\n",
					"#!/bin/sh",
					"# Fake claude for PolicyDefinitionsIT.",
					"here=$(dirname \"$0\")",
					"cat > \"$here/prompt\"",
					"if grep -q POLICY_REVIEW \"$here/prompt\"; then",
					"  cp \"$here/prompt\" \"$here/last-policy-prompt\"",
					"  cat \"$here/policy-reply.json\"",
					"else",
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
