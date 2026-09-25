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
public class LexicalFindingScopeTest extends AbstractLexicalAssistantTest {

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
}
