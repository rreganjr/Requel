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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.assistant.AbstractLexicalAssistantTest;
import com.rreganjr.requel.assistant.ai.AiProperties;
import com.rreganjr.requel.assistant.core.corpus.CorpusFinderAssistant;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingRepository;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.service.api.dto.IssueDto;
import com.rreganjr.requel.service.command.AnnotationCommandRegistrar;
import com.rreganjr.requel.service.command.AnnotationSources;

/**
 * Issue #266, stage 2: the AI corpus analysis through the worker, with a fake CLI. The bundled
 * {@code ai-corpus-relationships} definition runs over a project; each relationship finding is one
 * issue on every participant, raised once, idempotent, beside the per-entity reviews; an index
 * over the budget is refused; the finder's advisory issues on the judged pairs are replaced.
 */
@EnabledOnOs({ OS.LINUX, OS.MAC })
@TestPropertySource(properties = {
		"requel.ai.enabled=true",
		"requel.ai.provider=cli",
		"requel.ai.model=claude-cli",
		"spring.ai.model.chat=none",
		"requel.ai.cli.output-format=claude-json",
		"requel.ai.cli.timeout=30s" })
public class CorpusAnalysisIT extends AbstractLexicalAssistantTest {

	private static final String CORPUS = "ai-corpus-relationships";
	private static final Path FAKE_CLI_DIR = createFakeCli();

	@DynamicPropertySource
	static void cliCommand(DynamicPropertyRegistry registry) {
		registry.add("requel.ai.cli.command", () -> FAKE_CLI_DIR.resolve("fake-claude").toString());
	}

	@Autowired
	private CorpusAnalysisService corpusAnalysisService;

	@Autowired
	private AiReviewService aiReviewService;

	@Autowired
	private AssistantFindingRepository findingRepository;

	@Autowired
	private AiProperties aiProperties;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@AfterEach
	public void reset() throws IOException {
		SecurityContextHolder.clearContext();
		Files.deleteIfExists(FAKE_CLI_DIR.resolve("last-corpus-prompt"));
	}

	@Test
	public void aContradictionIsOneIssueNamingBothGoals() throws Exception {
		Project project = newProject("CorpusAI");
		Goal twoWeeks = loanGoal(project, "Two-week loans", 14);
		Goal threeWeeks = loanGoal(project, "Three-week loans", 21);
		newGoal(project, projectUser(), "Volunteers plan repairs " + stamp(),
				"The volunteer team meets every Monday to plan repairs.");
		corpusReply(List.of(finding("CONTRADICTORY_REQUIREMENTS",
				List.of(ref(twoWeeks), ref(threeWeeks)), List.of("14 days", "21 days"),
				"Two-week loans and Three-week loans set different loan lengths.")));

		AssistantRunEntity run = analyse(project);

		assertEquals("SUCCEEDED", run.getStatus(), run.getErrorSummary());
		String prompt = Files.readString(FAKE_CLI_DIR.resolve("last-corpus-prompt"));
		assertTrue(prompt.contains("\"candidates\"") && prompt.contains(ref(twoWeeks))
				&& prompt.contains("NUMBER"), "the finder's pair is sent with its hint");
		List<AssistantFindingEntity> rows = rows(project);
		assertEquals(2, rows.size());
		Long issueId = rows.get(0).getAppliedAnnotationId();
		assertEquals(issueId, rows.get(1).getAppliedAnnotationId());
		IssueDto dto = issue(issueId);
		assertEquals(2, dto.subjects().size(), "both participants, each resolving");
		assertEquals(AnnotationSources.CORPUS, dto.sourceKind());
		assertTrue(dto.mustBeResolved());
		assertEquals(0, run.getEvidenceUnverified(), "the quotes are in the participants' text");

		// the run read lists the finding on each participant
		signIn();
		var read = corpusAnalysisService.latest(project.getId(), "PROJECT", null, "ANALYSIS")
				.orElseThrow();
		assertEquals(2, read.findings().size());
		assertEquals(Set.of(twoWeeks.getId(), threeWeeks.getId()), new HashSet<>(read.findings()
				.stream().map(f -> f.targetId()).toList()));

		// a re-run on the unchanged project adds nothing
		analyse(project);
		assertEquals(2, rows(project).size());
		assertEquals(issueId, rows(project).get(0).getAppliedAnnotationId());
	}

	@Test
	public void aRelationshipIsRaisedOnceAndStrayParticipantsAreDropped() throws Exception {
		Project project = newProject("CorpusOnce");
		Goal a = newGoal(project, projectUser(), "Hold a tool " + stamp(),
				"A member can place a hold on a tool that is out on loan.");
		Goal b = newGoal(project, projectUser(), "Accounts on hold " + stamp(),
				"An account with unpaid fines is put on hold.");
		Goal c = newGoal(project, projectUser(), "Holds expire " + stamp(),
				"A hold lapses after three days.");
		corpusReply(List.of(
				finding("SAME_TERM_DIFFERENT_MEANING", List.of(ref(a), ref(b), ref(c)),
						List.of(), "'Hold' means a reservation in two goals and a block in one."),
				// the same relationship again, in another order: raised once
				finding("SAME_TERM_DIFFERENT_MEANING", List.of(ref(c), ref(b), ref(a)),
						List.of(), "Hold is used two ways."),
				// one participant outside the set, the other alone: dropped
				finding("OTHER", List.of(ref(a), "Goal:999999999"), List.of(), "Stray."),
				finding("OTHER", List.of(ref(a)), List.of(), "Alone.")));

		AssistantRunEntity run = analyse(project);

		assertEquals("SUCCEEDED", run.getStatus(), run.getErrorSummary());
		List<AssistantFindingEntity> rows = rows(project);
		assertEquals(3, rows.size(), "one relationship, three participants");
		Set<Long> issues = new HashSet<>();
		rows.forEach(row -> issues.add(row.getAppliedAnnotationId()));
		assertEquals(1, issues.size());
		assertEquals(3, issue(issues.iterator().next()).subjects().size());
	}

	@Test
	public void anIndexOverTheBudgetIsRefusedAndNothingIsAnalysed() throws Exception {
		Project project = newProject("CorpusBig");
		loanGoal(project, "Two-week loans", 14);
		loanGoal(project, "Three-week loans", 21);
		corpusReply(List.of());
		int before = aiProperties.getCorpus().getMaxInputChars();
		aiProperties.getCorpus().setMaxInputChars(100);
		try {
			AssistantRunEntity run = analyse(project);
			assertEquals("FAILED", run.getStatus());
			assertTrue(run.getErrorSummary().contains("The corpus index for Project")
					&& run.getErrorSummary().contains("requel.ai.corpus.max-input-chars"),
					run.getErrorSummary());
			assertFalse(Files.exists(FAKE_CLI_DIR.resolve("last-corpus-prompt")),
					"nothing was sent");
		} finally {
			aiProperties.getCorpus().setMaxInputChars(before);
		}
	}

	@Test
	public void corpusAndReviewFindingsCoexistAndTheFindersIssueIsReplaced() throws Exception {
		Project project = newProject("CorpusCoexist");
		Goal twoWeeks = loanGoal(project, "Two-week loans", 14);
		Goal threeWeeks = loanGoal(project, "Three-week loans", 21);

		// Find overlaps first: an advisory issue on the pair
		findOverlaps(project);
		List<AssistantFindingEntity> finder = findingRepository.findByAssistantIdAndProjectIdAndState(
				CorpusFinderAssistant.ASSISTANT_ID, project.getId(), "ACTIVE");
		assertEquals(2, finder.size());

		// a review of one goal, then the corpus analysis
		reviewReply(List.of(finding("UNMEASURABLE", null, List.of(), "No measure.")));
		review(twoWeeks);
		corpusReply(List.of(finding("CONTRADICTORY_REQUIREMENTS",
				List.of(ref(twoWeeks), ref(threeWeeks)), List.of(), "Different loan lengths.")));
		analyse(project);
		assertEquals(0, findingRepository.findByAssistantIdAndProjectIdAndState(
				CorpusFinderAssistant.ASSISTANT_ID, project.getId(), "ACTIVE").size(),
				"the analysis judged the pair, so the finder's issue is gone");

		// neither pass retires the other's findings
		reviewReply(List.of());
		review(twoWeeks);
		assertEquals(2, rows(project).size(), "a review re-run leaves the corpus issue");
		reviewReply(List.of(finding("UNMEASURABLE", null, List.of(), "No measure.")));
		review(twoWeeks);
		corpusReply(List.of());
		analyse(project);
		assertEquals(0, rows(project).size(), "the corpus re-run retires its own");
		assertEquals(1, findingRepository.findByAssistantIdAndProjectIdAndState("ai-review-goal",
				project.getId(), "ACTIVE").size(), "and leaves the review's");
	}

	private Goal loanGoal(Project project, String name, int days) throws Exception {
		return newGoal(project, projectUser(), name + " " + stamp(),
				"Every borrowed tool is due back " + days + " days after checkout.");
	}

	private AssistantRunEntity analyse(Project project) {
		return runCorpus(project, "ANALYSIS", CorpusAnalysisService.CORPUS_REVIEW);
	}

	private AssistantRunEntity findOverlaps(Project project) {
		return runCorpus(project, "CANDIDATES", CorpusFinderAssistant.TASK_TYPE);
	}

	private AssistantRunEntity runCorpus(Project project, String mode, String taskType) {
		signIn();
		corpusAnalysisService.request(project.getId(), "PROJECT", null, mode);
		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(r -> taskType.equals(r.getTaskType()) && "QUEUED".equals(r.getStatus()))
				.reduce((first, second) -> second).orElseThrow();
		assistantRunWorker.run(queued.getRunId());
		return assistantRunRepository.findById(queued.getId()).orElseThrow();
	}

	private void review(Goal goal) {
		signIn();
		aiReviewService.requestReview("Goal", goal.getId());
		for (AssistantRunEntity queued : assistantRunRepository.findAll()) {
			if (goal.getId().equals(queued.getTargetId()) && "QUEUED".equals(queued.getStatus())) {
				assistantRunWorker.run(queued.getRunId());
			}
		}
	}

	private static void signIn() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("project", null, List.of()));
	}

	private List<AssistantFindingEntity> rows(Project project) {
		return findingRepository.findByAssistantIdAndProjectIdAndState(CORPUS, project.getId(),
				"ACTIVE");
	}

	private IssueDto issue(Long issueId) {
		return new TransactionTemplate(transactionManager).execute(status -> AnnotationCommandRegistrar
				.toIssueDto(getAnnotationRepository().findById(Issue.class, issueId)));
	}

	private static String ref(Goal goal) {
		return "Goal:" + goal.getId();
	}

	private static Map<String, Object> finding(String type, List<String> participants,
			List<String> evidence, String issueText) {
		Map<String, Object> finding = new HashMap<>();
		finding.put("findingType", type);
		if (participants != null) {
			finding.put("participants", new ArrayList<>(participants));
		}
		finding.put("severity", "MEDIUM");
		finding.put("confidence", 0.8);
		finding.put("evidenceReferences", evidence);
		finding.put("suggestedIssueText", issueText);
		finding.put("suggestedNoteText", null);
		finding.put("suggestedPositions", List.of("Pick one."));
		finding.put("suggestedEntityName", null);
		return finding;
	}

	private void corpusReply(List<Map<String, Object>> findings) throws IOException {
		write("corpus-reply.json", findings);
	}

	private void reviewReply(List<Map<String, Object>> findings) throws IOException {
		write("reply.json", findings);
	}

	private void write(String file, List<Map<String, Object>> findings) throws IOException {
		String reply = objectMapper.writeValueAsString(Map.of("summary", findings.size()
				+ " findings", "findings", findings, "warnings", List.of()));
		Files.writeString(FAKE_CLI_DIR.resolve(file), objectMapper.writeValueAsString(
				Map.of("type", "result", "is_error", false, "result", reply,
						"total_cost_usd", 0.001,
						"usage", Map.of("input_tokens", 100, "output_tokens", 20))));
	}

	private static Path createFakeCli() {
		try {
			Path dir = Files.createTempDirectory("requel-fake-cli-");
			dir.toFile().deleteOnExit();
			Path script = dir.resolve("fake-claude");
			Files.writeString(script, String.join("\n",
					"#!/bin/sh",
					"# Fake claude for CorpusAnalysisIT.",
					"here=$(dirname \"$0\")",
					"cat > \"$here/prompt\"",
					"if grep -q CORPUS_REVIEW \"$here/prompt\"; then",
					"  cp \"$here/prompt\" \"$here/last-corpus-prompt\"",
					"  cat \"$here/corpus-reply.json\"",
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
