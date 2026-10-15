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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.annotation.impl.AbstractAnnotation;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.annotation.spi.AnnotationFreshness;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingState;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunRepository;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.impl.GoalImpl;

/**
 * Issue #270, end to end on the lexical spelling assistant: a finding records the fingerprint of
 * the text it was derived from, an edit makes it read stale as soon as the edit commits (before
 * any re-run), a re-run that reports it again makes it fresh, and a stale lexical issue that
 * carries a person's position is kept (SUPERSEDED, reads stale) rather than deleted.
 *
 * <p>
 * Same harness as {@link LexicalSpellingDispatchTest}: the test profile's assistant executor
 * discards the async task, so each edit leaves a QUEUED run the test drives synchronously.
 */
public class StaleFindingTest extends AbstractIntegrationTestCase {

	private AssistantRunWorker assistantRunWorker;
	private AssistantRunRepository assistantRunRepository;
	private AnnotationFreshness annotationFreshness;

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
	protected void setAnnotationFreshness(AnnotationFreshness annotationFreshness) {
		this.annotationFreshness = annotationFreshness;
	}

	@Test
	public void anEditMakesTheFindingStaleBeforeAnyReRunAndAReRunMakesItFresh() throws Exception {
		ensureDictionaryLoaded();
		User creator = getUserRepository().findUserByUsername("project");
		Goal goal = newGoal(creator, "Stale", "Test groal", "a clear requirement.");

		runLatestQueuedRun(goal.getId());
		AssistantFindingEntity finding = groalFindingFresh(goal.getId());
		assertNotNull(finding, "expected a 'groal' spelling finding after the first run");
		assertNotNull(finding.getTargetFingerprint(), "the finding records its fingerprint");
		Long issueId = finding.getAppliedAnnotationId();
		assertFalse(isStaleFresh(goal.getId(), issueId), "fresh right after the run");

		// Edit the text only: "groal" is still in the name, but the finding was derived from
		// the old text. Stale as soon as the command commits - no run has happened.
		edit(creator, goal, goalNameFresh(goal.getId()), "a clearer requirement.");
		assertTrue(isStaleFresh(goal.getId(), issueId),
				"an edited entity's finding reads stale before any re-run");

		// The run the edit queued reports the finding again: fresh, with the new fingerprint.
		runLatestQueuedRun(goal.getId());
		AssistantFindingEntity afterRun = groalFindingFresh(goal.getId());
		assertNotEquals(finding.getTargetFingerprint(), afterRun.getTargetFingerprint());
		assertEquals(AssistantFindingState.ACTIVE.name(), afterRun.getState());
		assertFalse(isStaleFresh(goal.getId(), issueId), "fresh again once re-reported");
	}

	@Test
	public void aStaleIssueWithAPersonsPositionIsKeptAndReadsStale() throws Exception {
		ensureDictionaryLoaded();
		User creator = getUserRepository().findUserByUsername("project");
		Goal goal = newGoal(creator, "Discussed", "Discuss groal", "a clear requirement.");

		runLatestQueuedRun(goal.getId());
		Issue issue = findGroalIssue(goal.getId());
		assertNotNull(issue, "expected an unknown-word issue after the first run");
		Long issueId = issue.getId();

		// A person joins the discussion with their own position.
		EditPositionCommand position = getAnnotationCommandFactory().newEditPositionCommand();
		position.setEditedBy(creator);
		position.setIssue(issue);
		position.setText("Groal is our product's code name " + System.nanoTime() + ".");
		getCommandHandler().execute(position);

		// Fix the spelling: the run no longer reports the finding. Before #270 the issue, and
		// the person's position with it, was deleted.
		edit(creator, goal, "Discuss goal " + System.nanoTime(), "a clear requirement.");
		runLatestQueuedRun(goal.getId());

		assertTrue(annotationExistsFresh(issueId), "human discussion is never deleted");
		AssistantFindingEntity finding = groalFindingFresh(goal.getId());
		assertEquals(AssistantFindingState.SUPERSEDED.name(), finding.getState(),
				"kept, the finding is SUPERSEDED rather than left ACTIVE");
		assertTrue(isStaleFresh(goal.getId(), issueId), "and the issue reads stale");

		// Resolving a SUPERSEDED finding's issue closes the finding.
		Issue kept = findGroalIssue(goal.getId());
		Position ignore = kept.getPositions().stream()
				.filter(p -> "Ignore this word.".equals(p.getText())).findFirst()
				.orElseThrow(() -> new AssertionError("no ignore position on the lexical issue"));
		ResolveIssueCommand resolve = getAnnotationCommandFactory().newResolveIssueCommand(ignore);
		resolve.setEditedBy(creator);
		resolve.setIssue(kept);
		resolve.setPosition(ignore);
		resolve.setAnnotatable(getProjectRepository().findById(Goal.class, goal.getId()));
		getCommandHandler().execute(resolve);
		assertEquals(AssistantFindingState.MANUALLY_RESOLVED.name(),
				groalFindingFresh(goal.getId()).getState());
	}

	@Test
	public void anUntouchedStaleIssueIsStillRemovedByTheReRun() throws Exception {
		ensureDictionaryLoaded();
		User creator = getUserRepository().findUserByUsername("project");
		Goal goal = newGoal(creator, "Untouched", "Plain groal", "a clear requirement.");

		runLatestQueuedRun(goal.getId());
		Long issueId = groalFindingFresh(goal.getId()).getAppliedAnnotationId();
		edit(creator, goal, "Plain goal " + System.nanoTime(), "a clear requirement.");
		runLatestQueuedRun(goal.getId());

		assertFalse(annotationExistsFresh(issueId),
				"assistant-suggested positions alone do not block the cleanup");
		assertEquals(AssistantFindingState.AUTO_RESOLVED.name(),
				groalFindingFresh(goal.getId()).getState());
	}

	private Goal newGoal(User creator, String label, String name, String text) throws Exception {
		long ts = System.nanoTime();
		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(creator);
		projectCommand.setName(label + " Stale Project " + ts);
		projectCommand.setOrganizationName(label + " Stale Org " + ts);
		projectCommand = getCommandHandler().execute(projectCommand);
		Project project = projectCommand.getProject();

		EditGoalCommand goalCommand = getProjectCommandFactory().newEditGoalCommand();
		goalCommand.setEditedBy(creator);
		goalCommand.setGoalContainer(project);
		goalCommand.setName(name + " " + ts); // "groal" is an intentional misspelling
		goalCommand.setText(text);
		return getCommandHandler().execute(goalCommand).getGoal();
	}

	private void edit(User creator, Goal goal, String name, String text) throws Exception {
		EditGoalCommand command = getProjectCommandFactory().newEditGoalCommand();
		command.setEditedBy(creator);
		command.setGoal(getProjectRepository().findById(Goal.class, goal.getId()));
		command.setName(name);
		command.setText(text);
		getCommandHandler().execute(command);
	}

	/** Staleness of one annotation on a goal, read through a fresh EntityManager. */
	private boolean isStaleFresh(Long goalId, Long annotationId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			GoalImpl goal = em.find(GoalImpl.class, goalId);
			AnnotationFreshness.StaleAnnotations stale = annotationFreshness
					.staleAnnotations(List.of(goal));
			for (Annotation annotation : goal.getAnnotations()) {
				if (annotationId.equals(annotation.getId())) {
					return stale.isStale(goal, annotation);
				}
			}
			throw new AssertionError("annotation " + annotationId + " is not on goal " + goalId);
		} finally {
			em.close();
		}
	}

	private String goalNameFresh(Long goalId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			return em.find(GoalImpl.class, goalId).getName();
		} finally {
			em.close();
		}
	}

	private Issue findGroalIssue(Long goalId) {
		Goal reloaded = getProjectRepository().findById(Goal.class, goalId);
		return reloaded.getAnnotations().stream()
				.filter(annotation -> annotation instanceof LexicalIssue
						&& annotation.getText() != null
						&& annotation.getText().contains("not recognized")
						&& annotation.getText().contains("groal"))
				.map(annotation -> (Issue) annotation).findFirst().orElse(null);
	}

	private AssistantFindingEntity groalFindingFresh(Long goalId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			List<AssistantFindingEntity> findings = em.createQuery(
					"select f from AssistantFindingEntity f where f.assistantId = :aid "
							+ "and f.targetType = :tt and f.targetId = :tid and f.summary like :sum",
					AssistantFindingEntity.class)
					.setParameter("aid", "legacy-lexical").setParameter("tt", "Goal")
					.setParameter("tid", goalId).setParameter("sum", "%groal%").getResultList();
			return findings.isEmpty() ? null : findings.get(0);
		} finally {
			em.close();
		}
	}

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
		assistantRunWorker.run(queued.getRunId());
	}
}
