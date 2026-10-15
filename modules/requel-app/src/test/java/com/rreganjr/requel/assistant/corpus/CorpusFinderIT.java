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
package com.rreganjr.requel.assistant.corpus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.rreganjr.requel.ai.CorpusAnalysisService;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.assistant.AbstractLexicalAssistantTest;
import com.rreganjr.requel.assistant.core.corpus.CorpusFinderAssistant;
import com.rreganjr.requel.assistant.core.freshness.FindingFreshness;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingRepository;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.service.api.dto.IssueDto;
import com.rreganjr.requel.service.command.AnnotationCommandRegistrar;
import com.rreganjr.requel.service.command.AnnotationSources;

/**
 * Issue #266, stage 1: "Find overlaps" through the worker. A conflicting pair becomes one issue on
 * both goals; a re-run adds nothing; editing either goal makes it stale on both; a fix retires it;
 * a smaller set leaves it alone; and an edit never starts a corpus run.
 */
public class CorpusFinderIT extends AbstractLexicalAssistantTest {

	@Autowired
	private CorpusAnalysisService corpusAnalysisService;

	@Autowired
	private AssistantFindingRepository findingRepository;

	@Autowired
	private FindingFreshness freshness;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@AfterEach
	public void reset() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void aConflictingPairIsOneIssueOnBothGoals() throws Exception {
		Project project = newProject("CorpusPair");
		Goal twoWeeks = newGoal(project, projectUser(), "Two-week loans " + stamp(),
				"Every borrowed tool is due back 14 days after checkout.");
		Goal threeWeeks = newGoal(project, projectUser(), "Three-week loans " + stamp(),
				"Every borrowed tool is due back 21 days after checkout.");
		Goal unrelated = newGoal(project, projectUser(), "Volunteers plan repairs " + stamp(),
				"The volunteer team meets every Monday to plan repairs.");

		AssistantRunEntity run = findOverlaps(project, "PROJECT", null);
		assertEquals("SUCCEEDED", run.getStatus(), run.getErrorSummary());

		List<AssistantFindingEntity> rows = corpusRows(project);
		assertEquals(2, rows.size(), "one row per participant");
		assertTrue(rows.stream().allMatch(AssistantFindingEntity::isStaleTogether));
		assertEquals(CorpusFinderAssistant.POSSIBLE_CONFLICT, rows.get(0).getFindingType());
		Long issueId = rows.get(0).getAppliedAnnotationId();
		assertEquals(issueId, rows.get(1).getAppliedAnnotationId(), "one shared issue");
		assertEquals(Set.of("GoalImpl:" + twoWeeks.getId(), "GoalImpl:" + threeWeeks.getId()),
				annotatablesOf(issueId));

		IssueDto dto = transaction().execute(status -> AnnotationCommandRegistrar
				.toIssueDto(getAnnotationRepository().findById(Issue.class, issueId)));
		assertTrue(dto.text().contains("'14 days' / '21 days'"), dto.text());
		assertEquals(2, dto.subjects().size());
		assertEquals(AnnotationSources.CORPUS, dto.sourceKind());
		assertEquals("Find overlaps", dto.sourceName());
		assertFalse(dto.mustBeResolved());

		// a re-run on the unchanged project adds nothing
		findOverlaps(project, "PROJECT", null);
		assertEquals(2, corpusRows(project).size());
		assertEquals(issueId, corpusRows(project).get(0).getAppliedAnnotationId());

		// editing one goal makes the issue stale on both
		editGoal(threeWeeks, projectUser(), threeWeeks.getName(),
				"Every borrowed power tool is due back 21 days after checkout.");
		assertTrue(staleOn(twoWeeks, issueId), "stale on the goal that didn't change too");
		assertTrue(staleOn(threeWeeks, issueId));
		assertFalse(staleOn(unrelated, issueId));
	}

	@Test
	public void aFixRetiresTheIssueButASmallerSetLeavesItAlone() throws Exception {
		Project project = newProject("CorpusFix");
		newGoal(project, projectUser(), "Two-week loans " + stamp(),
				"Every borrowed tool is due back 14 days after checkout.");
		Goal threeWeeks = newGoal(project, projectUser(), "Three-week loans " + stamp(),
				"Every borrowed tool is due back 21 days after checkout.");
		Goal unrelated = newGoal(project, projectUser(), "Volunteers plan repairs " + stamp(),
				"The volunteer team meets every Monday to plan repairs.");
		findOverlaps(project, "PROJECT", null);
		Long issueId = corpusRows(project).get(0).getAppliedAnnotationId();

		// the pair isn't in this set, so this run doesn't retire it
		editGoal(threeWeeks, projectUser(), "Renewals " + stamp(),
				"A member can renew once from the account page.");
		AssistantRunEntity subset = findOverlaps(project, "GOAL", unrelated.getId());
		assertEquals("SUCCEEDED", subset.getStatus(), subset.getErrorSummary());
		assertEquals(2, activeRows(project), "untouched by a run over another goal");

		findOverlaps(project, "PROJECT", null);
		assertEquals(0, activeRows(project), "retired once the project run no longer reports it");
		assertTrue(issueGone(issueId) || annotatablesOf(issueId).isEmpty(), "the issue went with it");
	}

	@Test
	public void anEditNeverStartsACorpusRun() throws Exception {
		Project project = newProject("CorpusEdit");
		long before = corpusRuns();
		Goal goal = newGoal(project, projectUser(), "Two-week loans " + stamp(),
				"Every borrowed tool is due back 14 days after checkout.");
		newGoal(project, projectUser(), "Three-week loans " + stamp(),
				"Every borrowed tool is due back 21 days after checkout.");
		editGoal(goal, projectUser(), goal.getName(), "Every tool is due back 14 days after checkout.");
		assertEquals(before, corpusRuns(), "an edit queued a corpus run");
		assertEquals(0, corpusRows(project).size(), "the post-edit runs never ran the finder");
	}

	private long corpusRuns() {
		return assistantRunRepository.findAll().stream()
				.filter(r -> r.getTaskType() != null && r.getTaskType().startsWith("CORPUS")).count();
	}

	/** Request "Find overlaps" as the project user and run what it queued. */
	private AssistantRunEntity findOverlaps(Project project, String set, Long rootId) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("project", null, List.of()));
		corpusAnalysisService.request(project.getId(), set, rootId, "CANDIDATES");
		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(r -> CorpusFinderAssistant.TASK_TYPE.equals(r.getTaskType())
						&& "QUEUED".equals(r.getStatus()))
				.reduce((first, second) -> second).orElseThrow();
		assistantRunWorker.run(queued.getRunId());
		return assistantRunRepository.findById(queued.getId()).orElseThrow();
	}

	private List<AssistantFindingEntity> corpusRows(Project project) {
		return findingRepository.findByAssistantIdAndProjectIdAndState(
				CorpusFinderAssistant.ASSISTANT_ID, project.getId(), "ACTIVE");
	}

	private long activeRows(Project project) {
		return corpusRows(project).size();
	}

	private boolean staleOn(Goal goal, Long issueId) {
		return Boolean.TRUE.equals(transaction().execute(status -> {
			Goal loaded = getProjectRepository().findById(Goal.class, goal.getId());
			Issue issue = getAnnotationRepository().findById(Issue.class, issueId);
			return freshness.staleAnnotations(List.<Annotatable>of(loaded)).isStale(loaded, issue);
		}));
	}

	private boolean issueGone(Long issueId) {
		return transaction().execute(status -> {
			try {
				return getAnnotationRepository().findById(Issue.class, issueId) == null;
			} catch (RuntimeException e) {
				return true;
			}
		});
	}

	private TransactionTemplate transaction() {
		return new TransactionTemplate(transactionManager);
	}
}
