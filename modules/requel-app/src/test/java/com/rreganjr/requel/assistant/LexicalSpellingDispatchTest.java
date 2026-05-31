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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.annotation.impl.AbstractAnnotation;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.AssistantDispatcher;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingRepository;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingState;
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
	private AssistantFindingRepository assistantFindingRepository;

	@PersistenceUnit
	private EntityManagerFactory entityManagerFactory;

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

	@Autowired
	protected void setAssistantFindingRepository(
			AssistantFindingRepository assistantFindingRepository) {
		this.assistantFindingRepository = assistantFindingRepository;
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

	/**
	 * AUTO_RESOLVE_IF_UNTOUCHED reconciliation (Phase 4.5 Step 6): when a re-run no
	 * longer reports a previously-raised lexical issue and the issue is still
	 * assistant-owned and unresolved, the applicator removes the annotation and marks
	 * the finding AUTO_RESOLVED — reproducing the legacy {@code removeUnneededLexicalIssues}.
	 */
	@Test
	public void editingGoalToFixMisspellingAutoResolvesStaleLexicalIssue() throws Exception {
		ensureDictionaryLoaded();
		long ts = System.currentTimeMillis();
		User creator = getUserRepository().findUserByUsername("project");

		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(creator);
		projectCommand.setName("AutoResolve Project " + ts);
		projectCommand.setOrganizationName("AutoResolve Org " + ts);
		projectCommand = getCommandHandler().execute(projectCommand);
		Project project = projectCommand.getProject();

		EditGoalCommand goalCommand = getProjectCommandFactory().newEditGoalCommand();
		goalCommand.setEditedBy(creator);
		goalCommand.setGoalContainer(project);
		goalCommand.setName("Test groal " + ts); // "groal" is an intentional misspelling
		goalCommand.setText("a clear requirement.");
		goalCommand = getCommandHandler().execute(goalCommand);
		Goal goal = goalCommand.getGoal();

		// First run raises the unknown-word issue and an ACTIVE finding. (Reads use a
		// fresh EntityManager so they reflect the worker's committed REQUIRES_NEW
		// transaction, not the test session's first-level cache.)
		runLatestQueuedRun(goal.getId());
		List<AssistantFindingEntity> active = spellingFindingsFresh(goal.getId(),
				AssistantFindingState.ACTIVE);
		assertFalse(active.isEmpty(), "expected an ACTIVE spelling finding after the first run");
		Long issueId = active.get(0).getAppliedAnnotationId();
		assertNotNull(issueId, "the spelling finding should reference its applied annotation");
		assertTrue(annotationExistsFresh(issueId), "the unknown-word issue should exist");

		// Fix the spelling; editing the goal dispatches a fresh analysis run.
		EditGoalCommand fixCommand = getProjectCommandFactory().newEditGoalCommand();
		fixCommand.setEditedBy(creator);
		fixCommand.setGoal(goal);
		fixCommand.setGoalContainer(project);
		fixCommand.setName("Test goal " + ts); // corrected
		fixCommand.setText("a clear requirement.");
		getCommandHandler().execute(fixCommand);
		runLatestQueuedRun(goal.getId());

		// The stale finding is AUTO_RESOLVED and its annotation removed. (markAutoResolved
		// runs only after the remove command succeeds, so the finding state is the
		// authoritative signal that reconciliation ran.)
		assertTrue(spellingFindingsFresh(goal.getId(), AssistantFindingState.ACTIVE).isEmpty(),
				"no ACTIVE spelling finding should remain after auto-resolution");
		assertFalse(
				spellingFindingsFresh(goal.getId(), AssistantFindingState.AUTO_RESOLVED).isEmpty(),
				"the stale finding should be marked AUTO_RESOLVED");
		assertFalse(annotationExistsFresh(issueId),
				"stale unknown-word issue should have been auto-resolved (removed)");
	}

	/**
	 * Auto-resolution must not delete a finding a human has acted on: once the issue
	 * is resolved (a position accepted), it is no longer "untouched", so a later
	 * re-run that omits it leaves the resolved issue and its finding in place.
	 */
	@Test
	public void humanResolvedLexicalIssueSurvivesAutoResolve() throws Exception {
		ensureDictionaryLoaded();
		long ts = System.currentTimeMillis();
		User creator = getUserRepository().findUserByUsername("project");

		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(creator);
		projectCommand.setName("Resolved Project " + ts);
		projectCommand.setOrganizationName("Resolved Org " + ts);
		projectCommand = getCommandHandler().execute(projectCommand);
		Project project = projectCommand.getProject();

		EditGoalCommand goalCommand = getProjectCommandFactory().newEditGoalCommand();
		goalCommand.setEditedBy(creator);
		goalCommand.setGoalContainer(project);
		goalCommand.setName("Resolve groal " + ts);
		goalCommand.setText("a clear requirement.");
		goalCommand = getCommandHandler().execute(goalCommand);
		Goal goal = goalCommand.getGoal();

		runLatestQueuedRun(goal.getId());
		Issue unknownWordIssue = findUnknownWordIssue(goal.getId());
		assertNotNull(unknownWordIssue, "expected an unknown-word issue after the first run");
		Long issueId = unknownWordIssue.getId();

		// A human accepts the "Ignore this word." position, resolving the issue.
		Position ignorePosition = unknownWordIssue.getPositions().stream()
				.filter(position -> "Ignore this word.".equals(position.getText())).findFirst()
				.orElseThrow(() -> new AssertionError("no ignore position on the lexical issue"));
		ResolveIssueCommand resolveCommand = getAnnotationCommandFactory()
				.newResolveIssueCommand(ignorePosition);
		resolveCommand.setEditedBy(creator);
		resolveCommand.setIssue(unknownWordIssue);
		resolveCommand.setPosition(ignorePosition);
		resolveCommand.setAnnotatable(goal);
		getCommandHandler().execute(resolveCommand);

		// Fix the spelling and re-run: the now-resolved issue must be preserved.
		EditGoalCommand fixCommand = getProjectCommandFactory().newEditGoalCommand();
		fixCommand.setEditedBy(creator);
		fixCommand.setGoal(goal);
		fixCommand.setGoalContainer(project);
		fixCommand.setName("Resolve goal " + ts);
		fixCommand.setText("a clear requirement.");
		getCommandHandler().execute(fixCommand);
		runLatestQueuedRun(goal.getId());

		assertTrue(annotationExistsFresh(issueId),
				"a human-resolved issue must not be auto-resolved away");
		assertTrue(
				spellingFindingsFresh(goal.getId(), AssistantFindingState.AUTO_RESOLVED).isEmpty(),
				"a resolved finding must not be marked AUTO_RESOLVED");
	}

	private Issue findUnknownWordIssue(Long goalId) {
		Goal reloaded = getProjectRepository().findById(Goal.class, goalId);
		return reloaded.getAnnotations().stream()
				.filter(annotation -> annotation instanceof LexicalIssue
						&& annotation.getText() != null
						&& annotation.getText().contains("not recognized")
						&& annotation.getText().contains("groal"))
				.map(annotation -> (Issue) annotation).findFirst().orElse(null);
	}

	/**
	 * Read findings through a fresh {@link EntityManager} so assertions reflect the
	 * worker's committed (REQUIRES_NEW) transaction rather than the test session's
	 * first-level cache.
	 */
	private List<AssistantFindingEntity> spellingFindingsFresh(Long goalId,
			AssistantFindingState state) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			// "legacy-lexical" == LexicalSpellingAssistant.ASSISTANT_ID
			return em.createQuery(
					"select f from AssistantFindingEntity f where f.assistantId = :aid "
							+ "and f.targetType = :tt and f.targetId = :tid and f.state = :st",
					AssistantFindingEntity.class)
					.setParameter("aid", "legacy-lexical").setParameter("tt", "Goal")
					.setParameter("tid", goalId).setParameter("st", state.name()).getResultList();
		} finally {
			em.close();
		}
	}

	/** True if an annotation with the given id still exists (fresh read). */
	private boolean annotationExistsFresh(Long annotationId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			return em.find(AbstractAnnotation.class, annotationId) != null;
		} finally {
			em.close();
		}
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
