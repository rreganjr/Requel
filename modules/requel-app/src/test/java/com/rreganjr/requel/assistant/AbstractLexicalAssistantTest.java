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
 * Issue #320: fixtures for driving the lexical assistants through the real SPI path (edit
 * command, queued run, {@link AssistantRunWorker}) and reading the committed results back.
 */
public abstract class AbstractLexicalAssistantTest extends AbstractIntegrationTestCase {


	/** {@code LexicalSpellingAssistant.ASSISTANT_ID}. */
	protected static final String SPELLING = "legacy-lexical";

	protected AssistantRunWorker assistantRunWorker;
	protected AssistantRunRepository assistantRunRepository;

	@PersistenceUnit
	protected EntityManagerFactory entityManagerFactory;

	@Autowired
	protected void setAssistantRunWorker(AssistantRunWorker assistantRunWorker) {
		this.assistantRunWorker = assistantRunWorker;
	}

	@Autowired
	protected void setAssistantRunRepository(AssistantRunRepository assistantRunRepository) {
		this.assistantRunRepository = assistantRunRepository;
	}

	// ---- fixtures -------------------------------------------------------------------------

	protected static String stamp() {
		return Long.toString(System.nanoTime());
	}

	protected User projectUser() {
		return getUserRepository().findUserByUsername("project");
	}

	protected Project newProject(String prefix) throws Exception {
		User user = projectUser();
		String ts = stamp();
		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(user);
		command.setName(prefix + " Project " + ts);
		command.setOrganizationName(prefix + " Org " + ts);
		return getCommandHandler().execute(command).getProject();
	}

	/** Create a goal and run the assistant run its edit queued. */
	protected Goal newGoal(Project project, User user, String name, String text) throws Exception {
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
	protected void editGoal(Goal goal, User user, String name, String text) throws Exception {
		EditGoalCommand command = getProjectCommandFactory().newEditGoalCommand();
		command.setEditedBy(user);
		command.setGoal(getProjectRepository().findById(Goal.class, goal.getId()));
		command.setName(name);
		command.setText(text);
		getCommandHandler().execute(command);
		runLatestQueuedRun(goal.getId());
	}

	protected void runLatestQueuedRun(Long goalId) {
		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(run -> "Goal".equals(run.getTargetType()) && goalId.equals(run.getTargetId())
						&& "QUEUED".equals(run.getStatus()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no QUEUED assistant run for goal " + goalId));
		assistantRunWorker.run(queued.getRunId());
	}

	protected Position positionWithText(Issue issue, String text) {
		return issue.getPositions().stream().filter(position -> text.equals(position.getText()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no '" + text + "' position on the issue"));
	}

	protected Position addPosition(Issue issue, User user, String text) throws Exception {
		EditPositionCommand command = getAnnotationCommandFactory().newEditPositionCommand();
		command.setEditedBy(user);
		command.setIssue(issue);
		command.setText(text);
		return getCommandHandler().execute(command).getPosition();
	}

	protected void resolve(Issue issue, Position position, Annotatable annotatable, User user)
			throws Exception {
		ResolveIssueCommand command = getAnnotationCommandFactory().newResolveIssueCommand(position);
		command.setEditedBy(user);
		command.setIssue(issue);
		command.setPosition(position);
		command.setAnnotatable(annotatable);
		getCommandHandler().execute(command);
	}

	protected void deleteIssue(Issue issue, User user) throws Exception {
		DeleteIssueCommand command = getAnnotationCommandFactory().newDeleteIssueCommand();
		command.setEditedBy(user);
		command.setIssue(issue);
		getCommandHandler().execute(command);
	}

	// ---- fresh reads (the worker commits in its own transactions) -------------------------

	protected String freshGoalName(Long goalId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			GoalImpl goal = em.find(GoalImpl.class, goalId);
			return goal == null ? "<missing>" : goal.getName();
		} finally {
			em.close();
		}
	}

	/** The goal's unknown-word issues for {@code word} in its Name. */
	protected List<LexicalIssue> spellingIssues(Long goalId, String word) {
		return lexicalIssues(goalId).stream()
				.filter(issue -> word.equalsIgnoreCase(issue.getWord())
						&& "Name".equals(issue.getAnnotatableEntityPropertyName())
						&& issue.getText() != null && issue.getText().contains("not recognized"))
				.collect(Collectors.toList());
	}

	protected Issue spellingIssue(Long goalId, String word) {
		List<LexicalIssue> issues = spellingIssues(goalId, word);
		return issues.isEmpty() ? null : issues.get(0);
	}

	/** The goal's glossary-term issue for {@code phrase}. */
	protected Issue glossaryIssue(Long goalId, String phrase) {
		return lexicalIssues(goalId).stream()
				.filter(issue -> phrase.equalsIgnoreCase(issue.getWord())
						&& issue.getAnnotatableEntityPropertyName() == null)
				.findFirst().orElse(null);
	}

	protected List<LexicalIssue> lexicalIssues(Long goalId) {
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
	protected Set<String> annotatablesOf(Long annotationId) {
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
	protected AssistantFindingEntity spellingFinding(Long goalId, String word) {
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
