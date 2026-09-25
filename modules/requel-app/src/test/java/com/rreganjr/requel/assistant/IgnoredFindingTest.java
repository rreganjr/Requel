/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.impl.IgnorePosition;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.IgnoredFinding;
import com.rreganjr.requel.project.IgnoredFindingStore;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.DeleteGoalCommand;
import com.rreganjr.requel.project.command.DeleteIgnoredFindingCommand;
import com.rreganjr.requel.project.command.DeleteProjectCommand;

/**
 * Issue #320: "Ignore" is recorded as an ignored finding, per entity and property, and the
 * assistants don't raise an ignored finding again, even after its resolved issue is deleted.
 */
public class IgnoredFindingTest extends AbstractLexicalAssistantTest {

	private static final String COMPLEX_SENTENCE = "When the customer who placed the order that"
			+ " the warehouse which the regional manager who reports to the director that the board"
			+ " appointed supervises shipped late asks the agent who handles the account that the"
			+ " sales team which the company acquired last year opened for a refund of the charge"
			+ " that the processor which the bank selected applied twice, the agent checks the"
			+ " policy.";

	@Autowired
	private IgnoredFindingStore ignoredFindingStore;

	@Test
	public void ignoringAWordRecordsAnIgnoredFindingWithAnIgnorePosition() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Record");
		Goal goal = newGoal(project, user, "groal intake " + stamp(), "The clerk records it.");

		Issue issue = spellingIssue(goal.getId(), "groal");
		assertNotNull(issue, "expected a 'groal' issue");
		Position ignore = positionWithText(issue, "Ignore this word.");
		assertTrue(Hibernate.unproxy(ignore) instanceof IgnorePosition,
				"the assistant's ignore position is an IgnorePosition");
		resolve(issue, ignore, goal, user);

		List<IgnoredFinding> ignored = ignoredFindingStore.list(project.getId());
		assertEquals(1, ignored.size(), "one ignore: " + ignored);
		IgnoredFinding row = ignored.get(0);
		assertEquals("Goal", row.getTargetType());
		assertEquals(goal.getId(), row.getTargetId());
		assertEquals("unknown-word", row.getFindingType());
		assertEquals("Name", row.getPropertyName());
		assertEquals("groal", row.getSubject());
		assertEquals(issue.getId(), row.getAnnotationId());
		assertEquals(SPELLING + ":Goal:" + goal.getId() + ":unknown-word:Name:groal",
				row.getIdempotencyKey());

		// A plain position doesn't record anything.
		Goal other = newGoal(project, user, "flarnix output " + stamp(), "The clerk prints it.");
		Issue otherIssue = spellingIssue(other.getId(), "flarnix");
		resolve(otherIssue, addPosition(otherIssue, user, "Fine as written " + stamp()), other,
				user);
		assertEquals(1, ignoredFindingStore.list(project.getId()).size());
	}

	@Test
	public void anIgnoredWordStaysIgnoredAfterItsIssueIsDeleted() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Survives");
		String ts = stamp();
		Goal goal = newGoal(project, user, "groal intake " + ts, "The clerk records it.");
		Issue issue = spellingIssue(goal.getId(), "groal");
		resolve(issue, positionWithText(issue, "Ignore this word."), goal, user);
		deleteIssue(issue, user);

		editGoal(goal, user, "groal intake " + ts, "The clerk records it twice.");
		assertNull(spellingIssue(goal.getId(), "groal"),
				"an ignored word is not raised again when its resolved issue is gone");
	}

	@Test
	public void anIgnoreIsCaseInsensitive() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Case");
		String ts = stamp();
		Goal goal = newGoal(project, user, "groal intake " + ts, "The clerk records it.");
		Issue issue = spellingIssue(goal.getId(), "groal");
		resolve(issue, positionWithText(issue, "Ignore this word."), goal, user);
		deleteIssue(issue, user);

		editGoal(goal, user, "Groal intake " + ts, "The clerk records it.");
		assertNull(spellingIssue(goal.getId(), "groal"), "'Groal' is ignored with 'groal'");
	}

	@Test
	public void anIgnoreCoversOneEntityAndPropertyInOneProject() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Scope");
		String ts = stamp();
		Goal first = newGoal(project, user, "groal intake " + ts, "The clerk records it.");
		Goal second = newGoal(project, user, "groal output " + ts, "The clerk prints it.");
		Issue firstIssue = spellingIssue(first.getId(), "groal");
		resolve(firstIssue, positionWithText(firstIssue, "Ignore this word."), first, user);

		// Another goal in the same project, same property: still raised, and still open.
		Issue secondIssue = spellingIssue(second.getId(), "groal");
		assertNotNull(secondIssue);
		assertFalse(secondIssue.isResolved(), "ignoring on one goal leaves the other open");

		// The same goal, another property: still raised.
		editGoal(first, user, "groal intake " + ts, "The clerk records the groal.");
		assertFalse(lexicalIssuesWhere(first.getId(), issue -> "groal".equalsIgnoreCase(
				issue.getWord()) && "Text".equals(issue.getAnnotatableEntityPropertyName()))
				.isEmpty(), "ignoring in Name leaves Text flagged");

		// Another project: still raised.
		Project otherProject = newProject("ScopeOther");
		Goal elsewhere = newGoal(otherProject, user, "groal intake " + ts, "The clerk records it.");
		assertNotNull(spellingIssue(elsewhere.getId(), "groal"));
		assertTrue(ignoredFindingStore.list(otherProject.getId()).isEmpty());
	}

	@Test
	public void anIgnoredGlossaryPhraseStaysIgnored() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Phrase");
		Goal goal = newGoal(project, user, "Intake " + stamp(), "The clerk completes the form.");
		Issue issue = glossaryIssue(goal.getId(), "the form");
		assertNotNull(issue, "expected a glossary issue for 'the form'");
		resolve(issue, positionWithText(issue, "Ignore this phrase."), goal, user);
		deleteIssue(issue, user);

		editGoal(goal, user, freshGoalName(goal.getId()), "The clerk completes the form now.");
		assertNull(glossaryIssue(goal.getId(), "the form"));
	}

	@Test
	public void anIgnoredComplexSentenceStaysIgnored() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Complex");
		Goal goal = newGoal(project, user, "Escalation " + stamp(), COMPLEX_SENTENCE);
		Predicate<LexicalIssue> complex = issue -> issue.getText() != null
				&& issue.getText().contains("is complex");
		List<LexicalIssue> issues = lexicalIssuesWhere(goal.getId(), complex);
		assertEquals(1, issues.size(), "expected one complexity issue");
		Issue issue = issues.get(0);
		resolve(issue, positionWithText(issue, "Ignore this word."), goal, user);
		deleteIssue(issue, user);

		editGoal(goal, user, freshGoalName(goal.getId()) + " again", COMPLEX_SENTENCE);
		assertTrue(lexicalIssuesWhere(goal.getId(), complex).isEmpty(),
				"the same sentence is not raised again");
	}

	@Test
	public void removingAnIgnoreRaisesTheFindingAgain() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Remove");
		String ts = stamp();
		Goal goal = newGoal(project, user, "groal intake " + ts, "The clerk records it.");
		Issue issue = spellingIssue(goal.getId(), "groal");
		resolve(issue, positionWithText(issue, "Ignore this word."), goal, user);
		IgnoredFinding ignored = ignoredFindingStore.list(project.getId()).get(0);

		DeleteIgnoredFindingCommand command = getProjectCommandFactory()
				.newDeleteIgnoredFindingCommand();
		command.setEditedBy(user);
		command.setProject(project);
		command.setIgnoredFindingId(ignored.getId());
		getCommandHandler().execute(command);

		assertTrue(ignoredFindingStore.list(project.getId()).isEmpty(), "the ignore is gone");
		assertNull(spellingIssue(goal.getId(), "groal"),
				"the resolved issue is unlinked from the goal");
		// The command queued a run for the goal; it raises the word again, open.
		runLatestQueuedRun(goal.getId());
		Issue reRaised = spellingIssue(goal.getId(), "groal");
		assertNotNull(reRaised, "the finding is raised again");
		assertFalse(reRaised.isResolved());
	}

	@Test
	public void deletingAGoalOrAProjectRemovesItsIgnores() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Delete");
		Project kept = newProject("DeleteKept");
		String ts = stamp();
		Goal doomed = newGoal(project, user, "groal intake " + ts, "The clerk records it.");
		Goal survivor = newGoal(project, user, "flarnix output " + ts, "The clerk prints it.");
		Goal elsewhere = newGoal(kept, user, "groal intake " + ts, "The clerk records it.");
		ignoreSpelling(doomed, user, "groal");
		ignoreSpelling(survivor, user, "flarnix");
		ignoreSpelling(elsewhere, user, "groal");
		assertEquals(2, ignoredFindingStore.list(project.getId()).size());

		DeleteGoalCommand deleteGoal = getProjectCommandFactory().newDeleteGoalCommand();
		deleteGoal.setEditedBy(user);
		deleteGoal.setGoal(getProjectRepository().findById(Goal.class, doomed.getId()));
		getCommandHandler().execute(deleteGoal);
		assertEquals(List.of(survivor.getId()), ignoredFindingStore.list(project.getId()).stream()
				.map(IgnoredFinding::getTargetId).collect(Collectors.toList()),
				"deleting a goal removes its ignores and no others");

		DeleteProjectCommand deleteProject = getProjectCommandFactory().newDeleteProjectCommand();
		deleteProject.setEditedBy(user);
		deleteProject.setProject(getProjectRepository().get(project));
		getCommandHandler().execute(deleteProject);
		assertTrue(ignoredFindingStore.list(project.getId()).isEmpty(),
				"deleting the project removes its ignores");
		assertEquals(1, ignoredFindingStore.list(kept.getId()).size(),
				"another project's ignores survive");
	}

	private void ignoreSpelling(Goal goal, User user, String word) throws Exception {
		Issue issue = spellingIssue(goal.getId(), word);
		assertNotNull(issue, "expected a '" + word + "' issue on goal " + goal.getId());
		resolve(issue, positionWithText(issue, "Ignore this word."), goal, user);
	}

	private List<LexicalIssue> lexicalIssuesWhere(Long goalId, Predicate<LexicalIssue> test) {
		return lexicalIssues(goalId).stream().filter(test).collect(Collectors.toList());
	}
}
