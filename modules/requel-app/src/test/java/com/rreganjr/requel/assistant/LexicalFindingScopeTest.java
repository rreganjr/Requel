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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.DeleteIssueCommand;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.annotation.impl.AbstractAnnotation;
import com.rreganjr.requel.annotation.impl.ChangeSpellingPosition;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
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
 * Issue #320: how an assistant's lexical issues relate to the entities they are about. Each test
 * drives the spelling and glossary assistants through the real SPI path (edit command, queued
 * run, {@link AssistantRunWorker}), the way the review reproduced them over MCP.
 * <ul>
 * <li>An ignored word stays ignored while its resolved issue exists (regression; this already
 * worked).</li>
 * <li>An issue belongs to one entity. The fallback lookups used to match project-wide, so a
 * second goal with the same word was handed the first goal's issue, and a Fix Spelling accepted
 * from one goal renamed the other.</li>
 * <li>A finding reported again with an open issue is {@code ACTIVE}, so it can auto-resolve
 * later.</li>
 * </ul>
 */
public class LexicalFindingScopeTest extends AbstractIntegrationTestCase {

	/** {@code LexicalSpellingAssistant.ASSISTANT_ID}. */
	private static final String SPELLING = "legacy-lexical";

	private AssistantRunWorker assistantRunWorker;
	private AssistantRunRepository assistantRunRepository;

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

	@Test
	public void ignoredWordIsNotReRaisedWhileItsResolvedIssueExists() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Ignore");
		Goal goal = newGoal(project, user, "groal intake " + stamp(), "The clerk records it.");

		Issue issue = spellingIssue(goal.getId(), "groal");
		assertNotNull(issue, "expected an unknown-word issue for 'groal'");
		resolve(issue, positionWithText(issue, "Ignore this word."), goal, user);

		editGoal(goal, user, freshGoalName(goal.getId()), "The clerk records it twice.");

		List<LexicalIssue> issues = spellingIssues(goal.getId(), "groal");
		assertEquals(1, issues.size(), "re-running must not add a second 'groal' issue");
		assertEquals(issue.getId(), issues.get(0).getId());
		assertTrue(issues.get(0).isResolved(), "the ignored issue must stay resolved");
	}

	@Test
	public void sameWordOnTwoGoalsGetsOneIssuePerGoal() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Scope");
		String ts = stamp();
		Goal first = newGoal(project, user, "groal intake " + ts, "The clerk completes the form.");
		Goal second = newGoal(project, user, "groal output " + ts, "The clerk completes the form.");

		Issue firstSpelling = spellingIssue(first.getId(), "groal");
		Issue secondSpelling = spellingIssue(second.getId(), "groal");
		assertNotNull(firstSpelling, "first goal: expected a 'groal' issue");
		assertNotNull(secondSpelling, "second goal: expected a 'groal' issue");
		assertNotEquals(firstSpelling.getId(), secondSpelling.getId(),
				"each goal must get its own 'groal' issue");
		assertEquals(Set.of("GoalImpl:" + first.getId()), annotatablesOf(firstSpelling.getId()));
		assertEquals(Set.of("GoalImpl:" + second.getId()), annotatablesOf(secondSpelling.getId()));

		// A glossary phrase the two goals share goes through the same fallback lookup.
		Issue firstPhrase = glossaryIssue(first.getId(), "the form");
		Issue secondPhrase = glossaryIssue(second.getId(), "the form");
		assertNotNull(firstPhrase, "first goal: expected a glossary issue for 'the form'");
		assertNotNull(secondPhrase, "second goal: expected a glossary issue for 'the form'");
		assertNotEquals(firstPhrase.getId(), secondPhrase.getId(),
				"each goal must get its own glossary issue");
		assertEquals(Set.of("GoalImpl:" + second.getId()), annotatablesOf(secondPhrase.getId()));
	}

	@Test
	public void fixSpellingOnOneGoalLeavesTheOtherGoalAlone() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("FixSpelling");
		String ts = stamp();
		Goal first = newGoal(project, user, "groal intake " + ts, "The clerk records it.");
		Goal second = newGoal(project, user, "groal output " + ts, "The clerk prints it.");

		Issue secondIssue = spellingIssue(second.getId(), "groal");
		assertNotNull(secondIssue, "second goal: expected a 'groal' issue");
		Position fix = secondIssue.getPositions().stream()
				.filter(position -> Hibernate.unproxy(position) instanceof ChangeSpellingPosition)
				.findFirst()
				.orElseThrow(() -> new AssertionError("no Fix Spelling position on the issue"));
		// No annotatable, as over the API: ResolveIssueInput carries only the issue and position,
		// so the resolver rewrites every entity the issue is attached to.
		resolve(secondIssue, fix, null, user);

		assertFalse(freshGoalName(second.getId()).contains("groal"),
				"the goal whose issue was fixed is renamed");
		assertEquals("groal intake " + ts, freshGoalName(first.getId()),
				"the other goal must keep its name");
	}

	@Test
	public void findingReRaisedAfterItsIssueIsDeletedIsActiveAndAutoResolves() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Stuck");
		String ts = stamp();
		Goal goal = newGoal(project, user, "groal intake " + ts, "The clerk records it.");

		// Resolve with a human position, then delete the resolved issue.
		Issue issue = spellingIssue(goal.getId(), "groal");
		assertNotNull(issue, "expected a 'groal' issue");
		resolve(issue, addPosition(issue, user, "Fine as written " + ts), goal, user);
		assertEquals(AssistantFindingState.MANUALLY_RESOLVED.name(),
				spellingFinding(goal.getId(), "groal").getState());
		deleteIssue(issue, user);

		// The next run raises the word again, as an open issue with an ACTIVE finding.
		editGoal(goal, user, "groal intake " + ts, "The clerk records it twice.");
		Issue reRaised = spellingIssue(goal.getId(), "groal");
		assertNotNull(reRaised, "the word is raised again once its issue is deleted");
		assertFalse(reRaised.isResolved());
		AssistantFindingEntity finding = spellingFinding(goal.getId(), "groal");
		assertEquals(AssistantFindingState.ACTIVE.name(), finding.getState(),
				"a finding reported again with an open issue must be ACTIVE");
		assertEquals(reRaised.getId(), finding.getAppliedAnnotationId());

		// So fixing the word cleans the issue up, which it never did while the finding was stuck.
		editGoal(goal, user, "goal intake " + ts, "The clerk records it twice.");
		assertNull(spellingIssue(goal.getId(), "groal"),
				"the re-raised issue must auto-resolve once the word is fixed");
		assertEquals(AssistantFindingState.AUTO_RESOLVED.name(),
				spellingFinding(goal.getId(), "groal").getState());
	}

	@Test
	public void wordThatComesBackReactivatesItsFinding() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Returns");
		String ts = stamp();
		Goal goal = newGoal(project, user, "groal intake " + ts, "The clerk records it.");
		assertNotNull(spellingIssue(goal.getId(), "groal"));

		editGoal(goal, user, "goal intake " + ts, "The clerk records it.");
		assertNull(spellingIssue(goal.getId(), "groal"));
		assertEquals(AssistantFindingState.AUTO_RESOLVED.name(),
				spellingFinding(goal.getId(), "groal").getState());

		editGoal(goal, user, "groal intake " + ts, "The clerk records it.");
		assertNotNull(spellingIssue(goal.getId(), "groal"), "the word is back, so is the issue");
		assertEquals(AssistantFindingState.ACTIVE.name(),
				spellingFinding(goal.getId(), "groal").getState());

		editGoal(goal, user, "goal intake " + ts, "The clerk records it.");
		assertNull(spellingIssue(goal.getId(), "groal"),
				"removed again, the issue must auto-resolve again");
		assertEquals(AssistantFindingState.AUTO_RESOLVED.name(),
				spellingFinding(goal.getId(), "groal").getState());
	}

	// ---- fixtures -------------------------------------------------------------------------

	private static String stamp() {
		return Long.toString(System.nanoTime());
	}

	private User projectUser() {
		return getUserRepository().findUserByUsername("project");
	}

	private Project newProject(String prefix) throws Exception {
		User user = projectUser();
		String ts = stamp();
		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(user);
		command.setName(prefix + " Project " + ts);
		command.setOrganizationName(prefix + " Org " + ts);
		return getCommandHandler().execute(command).getProject();
	}

	/** Create a goal and run the assistant run its edit queued. */
	private Goal newGoal(Project project, User user, String name, String text) throws Exception {
		EditGoalCommand command = getProjectCommandFactory().newEditGoalCommand();
		command.setEditedBy(user);
		command.setGoalContainer(project);
		command.setName(name);
		command.setText(text);
		Goal goal = getCommandHandler().execute(command).getGoal();
		runLatestQueuedRun(goal.getId());
		return goal;
	}

	/** Edit a goal and run the assistant run the edit queued. */
	private void editGoal(Goal goal, User user, String name, String text) throws Exception {
		EditGoalCommand command = getProjectCommandFactory().newEditGoalCommand();
		command.setEditedBy(user);
		command.setGoal(getProjectRepository().findById(Goal.class, goal.getId()));
		command.setName(name);
		command.setText(text);
		getCommandHandler().execute(command);
		runLatestQueuedRun(goal.getId());
	}

	private void runLatestQueuedRun(Long goalId) {
		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(run -> "Goal".equals(run.getTargetType()) && goalId.equals(run.getTargetId())
						&& "QUEUED".equals(run.getStatus()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no QUEUED assistant run for goal " + goalId));
		assistantRunWorker.run(queued.getRunId());
	}

	private Position positionWithText(Issue issue, String text) {
		return issue.getPositions().stream().filter(position -> text.equals(position.getText()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no '" + text + "' position on the issue"));
	}

	private Position addPosition(Issue issue, User user, String text) throws Exception {
		EditPositionCommand command = getAnnotationCommandFactory().newEditPositionCommand();
		command.setEditedBy(user);
		command.setIssue(issue);
		command.setText(text);
		return getCommandHandler().execute(command).getPosition();
	}

	private void resolve(Issue issue, Position position, Annotatable annotatable, User user)
			throws Exception {
		ResolveIssueCommand command = getAnnotationCommandFactory().newResolveIssueCommand(position);
		command.setEditedBy(user);
		command.setIssue(issue);
		command.setPosition(position);
		command.setAnnotatable(annotatable);
		getCommandHandler().execute(command);
	}

	private void deleteIssue(Issue issue, User user) throws Exception {
		DeleteIssueCommand command = getAnnotationCommandFactory().newDeleteIssueCommand();
		command.setEditedBy(user);
		command.setIssue(issue);
		getCommandHandler().execute(command);
	}

	// ---- fresh reads (the worker commits in its own transactions) -------------------------

	private String freshGoalName(Long goalId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			GoalImpl goal = em.find(GoalImpl.class, goalId);
			return goal == null ? "<missing>" : goal.getName();
		} finally {
			em.close();
		}
	}

	/** The goal's unknown-word issues for {@code word} in its Name. */
	private List<LexicalIssue> spellingIssues(Long goalId, String word) {
		return lexicalIssues(goalId).stream()
				.filter(issue -> word.equalsIgnoreCase(issue.getWord())
						&& "Name".equals(issue.getAnnotatableEntityPropertyName())
						&& issue.getText() != null && issue.getText().contains("not recognized"))
				.collect(Collectors.toList());
	}

	private Issue spellingIssue(Long goalId, String word) {
		List<LexicalIssue> issues = spellingIssues(goalId, word);
		return issues.isEmpty() ? null : issues.get(0);
	}

	/** The goal's glossary-term issue for {@code phrase}. */
	private Issue glossaryIssue(Long goalId, String phrase) {
		return lexicalIssues(goalId).stream()
				.filter(issue -> phrase.equalsIgnoreCase(issue.getWord())
						&& issue.getAnnotatableEntityPropertyName() == null)
				.findFirst().orElse(null);
	}

	private List<LexicalIssue> lexicalIssues(Long goalId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			GoalImpl goal = em.find(GoalImpl.class, goalId);
			List<LexicalIssue> issues = goal.getAnnotations().stream()
					.map(Hibernate::unproxy)
					.filter(LexicalIssue.class::isInstance).map(LexicalIssue.class::cast)
					.collect(Collectors.toList());
			// touch what the assertions read before the entity manager closes
			issues.forEach(issue -> {
				issue.isResolved();
				Hibernate.initialize(issue.getPositions());
			});
			return issues;
		} finally {
			em.close();
		}
	}

	/** "SimpleClassName:id" for every entity the annotation is attached to. */
	private Set<String> annotatablesOf(Long annotationId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			AbstractAnnotation annotation = em.find(AbstractAnnotation.class, annotationId);
			return annotation.getAnnotatables().stream().map(Hibernate::unproxy)
					.map(entity -> entity.getClass().getSimpleName() + ":"
							+ ((com.rreganjr.requel.project.ProjectOrDomainEntity) entity).getId())
					.collect(Collectors.toSet());
		} finally {
			em.close();
		}
	}

	/** The spelling finding for {@code word} in the goal's Name. */
	private AssistantFindingEntity spellingFinding(Long goalId, String word) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			String key = SPELLING + ":Goal:" + goalId + ":unknown-word:Name:" + word;
			List<AssistantFindingEntity> findings = em
					.createQuery("select f from AssistantFindingEntity f where f.idempotencyKey = :key",
							AssistantFindingEntity.class)
					.setParameter("key", key).getResultList();
			assertFalse(findings.isEmpty(), "no finding " + key);
			return findings.get(0);
		} finally {
			em.close();
		}
	}
}
