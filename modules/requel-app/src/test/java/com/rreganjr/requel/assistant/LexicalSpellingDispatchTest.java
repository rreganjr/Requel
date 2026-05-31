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

import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.AssistantDispatcher;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunRepository;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;

/**
 * End-to-end coverage of the assistant SPI loop for the migrated Goal path
 * (issue #43, Phase 4.5 Step 5):
 *
 * <pre>
 *   edit Goal command -&gt; invokeAnalysis -&gt; AssistantDispatcher.dispatch
 *     -&gt; AssistantRun (QUEUED) -&gt; AssistantRunWorker -&gt; LexicalSpellingAssistant
 *     -&gt; AnnotationActions -&gt; CommandBackedAssistantResultApplicator -&gt; annotations
 * </pre>
 *
 * In the test profile {@code assistantTaskExecutor} is a no-op, so the command's
 * dispatch only persists the QUEUED run; the test then drives the worker
 * synchronously (what the async executor does in production) and asserts the
 * resulting annotations and run status.
 */
public class LexicalSpellingDispatchTest extends AbstractIntegrationTestCase {

	private AssistantRunWorker assistantRunWorker;
	private AssistantRunRepository assistantRunRepository;
	private AssistantDispatcher assistantDispatcher;

	@Autowired
	protected void setAssistantRunWorker(AssistantRunWorker assistantRunWorker) {
		this.assistantRunWorker = assistantRunWorker;
	}

	@Autowired
	protected void setAssistantRunRepository(AssistantRunRepository assistantRunRepository) {
		this.assistantRunRepository = assistantRunRepository;
	}

	@Autowired
	protected void setAssistantDispatcher(AssistantDispatcher assistantDispatcher) {
		this.assistantDispatcher = assistantDispatcher;
	}

	@Test
	public void editingGoalDispatchesAndAppliesLexicalAnnotations() throws Exception {
		ensureDictionaryLoaded();
		long ts = System.currentTimeMillis();
		User creator = getUserRepository().findUserByUsername("project");

		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(creator);
		projectCommand.setName("Dispatch Project " + ts);
		projectCommand.setOrganizationName("Dispatch Org " + ts);
		projectCommand = getCommandHandler().execute(projectCommand);
		Project project = projectCommand.getProject();

		EditGoalCommand goalCommand = getProjectCommandFactory().newEditGoalCommand();
		goalCommand.setEditedBy(creator);
		goalCommand.setGoalContainer(project);
		goalCommand.setName("Test groal " + ts); // "groal" is an intentional misspelling
		goalCommand.setText("a clear requirement.");
		goalCommand = getCommandHandler().execute(goalCommand);
		Goal goal = goalCommand.getGoal();

		// The command's invokeAnalysis dispatched a run through the SPI; the test
		// executor discards the async task, so the run is left QUEUED.
		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(run -> "Goal".equals(run.getTargetType())
						&& goal.getId().equals(run.getTargetId()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no assistant run was queued for the goal"));
		assertEquals("QUEUED", queued.getStatus());

		// Drive the worker synchronously (production runs it on the async executor).
		assistantRunWorker.runInNewTransaction(queued.getRunId());

		AssistantRunEntity completed = assistantRunRepository.findById(queued.getId())
				.orElseThrow(() -> new AssertionError("assistant run row vanished"));
		assertEquals("SUCCEEDED", completed.getStatus());
		assertTrue(completed.getFindingsCount() >= 1, "expected at least one finding");

		Goal reloaded = getProjectRepository().findById(Goal.class, goal.getId());
		boolean hasUnknownWordIssue = reloaded.getAnnotations().stream()
				.anyMatch(annotation -> annotation instanceof Issue && annotation.getText() != null
						&& annotation.getText().contains("groal"));
		assertTrue(hasUnknownWordIssue,
				"expected an unknown-word issue mentioning the misspelled word");
	}

	/**
	 * Re-running analysis for the same target must not duplicate findings: the
	 * AssistantFinding upsert (keyed by action key) plus the applicator's
	 * content-level dedupe reuse the existing annotation. (Phase 4.5 exit
	 * criterion.)
	 */
	@Test
	public void reRunningAnalysisDoesNotDuplicateFindings() throws Exception {
		ensureDictionaryLoaded();
		long ts = System.currentTimeMillis();
		User creator = getUserRepository().findUserByUsername("project");
		User assistantUser = getUserRepository().findUserByUsername("assistant");

		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(creator);
		projectCommand.setName("Idempotency Project " + ts);
		projectCommand.setOrganizationName("Idempotency Org " + ts);
		projectCommand = getCommandHandler().execute(projectCommand);
		Project project = projectCommand.getProject();

		EditGoalCommand goalCommand = getProjectCommandFactory().newEditGoalCommand();
		goalCommand.setEditedBy(creator);
		goalCommand.setGoalContainer(project);
		goalCommand.setName("Another groal " + ts);
		goalCommand.setText("a clear requirement.");
		goalCommand = getCommandHandler().execute(goalCommand);
		Goal goal = goalCommand.getGoal();

		// First analysis (the command already queued it).
		runLatestQueuedRun(goal.getId());
		long afterFirstRun = countUnknownWordIssues(goal.getId());
		assertTrue(afterFirstRun >= 1, "expected an unknown-word issue after the first run");

		// Dispatch a second analysis for the same goal and run it.
		AnalysisRequest request = new AnalysisRequest(EntityRef.of("Goal", goal.getId()),
				EntityRef.of("Project", project.getId()),
				new UserRef(creator.getId(), creator.getUsername()),
				new UserRef(assistantUser.getId(), assistantUser.getUsername()), null,
				Locale.getDefault(), Map.of());
		assistantDispatcher.dispatch(request).toCompletableFuture().get();
		runLatestQueuedRun(goal.getId());

		long afterSecondRun = countUnknownWordIssues(goal.getId());
		assertEquals(afterFirstRun, afterSecondRun,
				"re-running analysis must not duplicate the unknown-word issue");
	}

	private void runLatestQueuedRun(Long goalId) {
		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(run -> "Goal".equals(run.getTargetType()) && goalId.equals(run.getTargetId())
						&& "QUEUED".equals(run.getStatus()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no QUEUED assistant run for the goal"));
		assistantRunWorker.runInNewTransaction(queued.getRunId());
	}

	private long countUnknownWordIssues(Long goalId) {
		Goal reloaded = getProjectRepository().findById(Goal.class, goalId);
		return reloaded.getAnnotations().stream()
				.filter(annotation -> annotation instanceof Issue && annotation.getText() != null
						&& annotation.getText().contains("groal"))
				.count();
	}
}
