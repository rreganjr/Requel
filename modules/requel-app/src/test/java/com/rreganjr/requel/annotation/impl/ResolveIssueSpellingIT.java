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
package com.rreganjr.requel.annotation.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.command.EditChangeSpellingPositionCommand;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Fix Spelling behaviour after it stopped writing through reflective setters (issue #305).
 * <p>
 * The correction now goes through the annotated entity's own {@code Edit*Command}, resolved
 * from the text-editor registry, which is what makes it authorized, versioned and audited.
 * These cover the three things that change as a result: the entity's other text survives the
 * edit, a user who cannot edit the entity cannot correct it either, and an entity with no
 * registered editor refuses rather than being edited some other way.
 *
 * @author ron
 */
public class ResolveIssueSpellingIT extends AbstractIntegrationTestCase {

	/**
	 * The hazard behind issue #316, pinned here because #305 depends on it not biting.
	 * <p>
	 * The registry supplies only the corrected property, so a name correction reaches
	 * {@code EditGoalCommand} with a null text. The command's "null leaves it as it is" guard
	 * is what keeps the text; if that guard ever regresses, this fails rather than silently
	 * emptying a goal.
	 */
	@Test
	public void correctingTheNameLeavesTheTextIntact() throws Exception {
		Project project = createProject("spelling-name-only");
		User admin = getUserRepository().findUserByUsername("admin");
		Goal goal = createGoal(project, "Track systm health", "Body text that must survive.");

		resolveSpelling(project, goal, "Name", "systm", "system", admin);

		Goal reloaded = getProjectRepository()
				.findGoalByProjectOrDomainAndName(project, "Track system health");
		assertEquals("Body text that must survive.", reloaded.getText(),
				"correcting the name must not disturb the text");
		assertTrue(reloaded.getName().contains("system"), "the name should be corrected");
	}

	/** The mirror of {@link #correctingTheNameLeavesTheTextIntact()} (issue #316). */
	@Test
	public void correctingTheTextLeavesTheNameIntact() throws Exception {
		Project project = createProject("spelling-text-only");
		User admin = getUserRepository().findUserByUsername("admin");
		Goal goal = createGoal(project, "Name that must survive", "Users recieve a receipt.");

		resolveSpelling(project, goal, "Text", "recieve", "receive", admin);

		Goal reloaded = getProjectRepository()
				.findGoalByProjectOrDomainAndName(project, "Name that must survive");
		assertTrue(reloaded.getText().contains("receive"), "the text should be corrected");
		assertFalse(reloaded.getText().contains("recieve"), "the misspelling should be gone");
	}

	/**
	 * Holding Annotation[Edit] lets a user resolve issues; it does not let them rewrite a goal
	 * they cannot edit. The refusal happens before any write, so the goal and the issue are
	 * both left alone.
	 */
	@Test
	public void refusesToCorrectAnEntityTheUserCannotEdit() throws Exception {
		Project project = createProject("spelling-refused");
		User admin = getUserRepository().findUserByUsername("admin");
		Goal goal = createGoal(project, "Reduce systm latency", "Latency body text.");

		String username = "spelling-noedit-" + System.nanoTime();
		createProjectUser(username);
		// Annotation[Edit] only: enough to resolve, not enough to edit the goal.
		addStakeholder(project, username, Set.of(StakeholderPermissionImpl
				.generatePermissionKey(Annotation.class, StakeholderPermissionType.Edit)));
		User restricted = getUserRepository().findUserByUsername(username);

		LexicalIssue issue = newLexicalIssue(project, goal, "Name", "systm", admin);
		ChangeSpellingPosition position = newSpellingPosition(issue, "system", admin);

		ResolveIssueCommand resolve = getAnnotationCommandFactory()
				.newResolveIssueCommand(position);
		resolve.setEditedBy(restricted);
		resolve.setIssue(issue);
		resolve.setPosition(position);
		resolve.setAnnotatable(goal);

		assertThrows(AuthorizationException.class, () -> getCommandHandler().execute(resolve),
				"resolving must be refused without Goal[Edit]");

		Goal reloaded = getProjectRepository()
				.findGoalByProjectOrDomainAndName(project, "Reduce systm latency");
		assertTrue(reloaded.getName().contains("systm"),
				"the goal name must be untouched when the resolve is refused");
		Issue stillOpen = getAnnotationRepository().findIssue(project, goal, issueText("systm"));
		assertFalse(stillOpen.isResolved(),
				"the issue must stay unresolved when the correction is refused");
	}

	/**
	 * A glossary term is annotatable but no assistant analyses one, so it has no registered
	 * text editor. Resolving a spelling issue on one refuses with a clear error instead of
	 * falling back to editing it some other way.
	 */
	@Test
	public void refusesWhenTheAnnotatableHasNoRegisteredTextEditor() throws Exception {
		Project project = createProject("spelling-unregistered");
		User admin = getUserRepository().findUserByUsername("admin");

		EditGlossaryTermCommand termCmd = getProjectCommandFactory().newEditGlossaryTermCommand();
		termCmd.setEditedBy(admin);
		termCmd.setProjectOrDomain(project);
		termCmd.setName("systm");
		termCmd = getCommandHandler().execute(termCmd);
		GlossaryTerm term = termCmd.getGlossaryTerm();

		EditLexicalIssueCommand lexCmd = getAnnotationCommandFactory().newEditLexicalIssueCommand();
		lexCmd.setEditedBy(admin);
		lexCmd.setGroupingObject(project);
		lexCmd.setAnnotatable(term);
		lexCmd.setText(issueText("systm"));
		lexCmd.setMustBeResolved(false);
		lexCmd.setWord("systm");
		lexCmd.setAnnotatableEntityPropertyName("Name");
		lexCmd = getCommandHandler().execute(lexCmd);
		LexicalIssue issue = (LexicalIssue) lexCmd.getIssue();

		ChangeSpellingPosition position = newSpellingPosition(issue, "system", admin);

		ResolveIssueCommand resolve = getAnnotationCommandFactory()
				.newResolveIssueCommand(position);
		resolve.setEditedBy(admin);
		resolve.setIssue(issue);
		resolve.setPosition(position);
		resolve.setAnnotatable(term);

		assertThrows(IllegalArgumentException.class,
				() -> getCommandHandler().execute(resolve),
				"an annotatable with no registered text editor must refuse, not edit");
	}

	// -------------------------------------------------------------------------
	// Fixture helpers
	// -------------------------------------------------------------------------

	private static String issueText(String word) {
		return "Possible misspelling: " + word;
	}

	private void resolveSpelling(Project project, Goal goal, String propertyName, String from,
			String to, User editedBy) throws Exception {
		LexicalIssue issue = newLexicalIssue(project, goal, propertyName, from, editedBy);
		ChangeSpellingPosition position = newSpellingPosition(issue, to, editedBy);

		ResolveIssueCommand resolve = getAnnotationCommandFactory()
				.newResolveIssueCommand(position);
		resolve.setEditedBy(editedBy);
		resolve.setIssue(issue);
		resolve.setPosition(position);
		resolve.setAnnotatable(goal);
		getCommandHandler().execute(resolve);
	}

	private LexicalIssue newLexicalIssue(Project project, Object annotatable,
			String propertyName, String word, User editedBy) throws Exception {
		EditLexicalIssueCommand cmd = getAnnotationCommandFactory().newEditLexicalIssueCommand();
		cmd.setEditedBy(editedBy);
		cmd.setGroupingObject(project);
		cmd.setAnnotatable((com.rreganjr.requel.annotation.Annotatable) annotatable);
		cmd.setText(issueText(word));
		cmd.setMustBeResolved(false);
		cmd.setWord(word);
		cmd.setAnnotatableEntityPropertyName(propertyName);
		cmd = getCommandHandler().execute(cmd);
		return (LexicalIssue) cmd.getIssue();
	}

	private ChangeSpellingPosition newSpellingPosition(LexicalIssue issue, String proposedWord,
			User editedBy) throws Exception {
		EditChangeSpellingPositionCommand cmd = getAnnotationCommandFactory()
				.newEditChangeSpellingPositionCommand();
		cmd.setEditedBy(editedBy);
		cmd.setIssue(issue);
		cmd.setText("Change to '" + proposedWord + "'");
		cmd.setProposedWord(proposedWord);
		cmd = getCommandHandler().execute(cmd);
		return (ChangeSpellingPosition) cmd.getPosition();
	}

	private Project createProject(String label) throws Exception {
		long ts = System.nanoTime();
		User admin = getUserRepository().findUserByUsername("admin");
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(admin);
		cmd.setName(label + "-" + ts);
		cmd.setText("test project for " + label);
		cmd.setOrganizationName("SpellingTestOrg-" + ts);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	private Goal createGoal(Project project, String name, String text) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(admin);
		cmd.setGoalContainer(project);
		cmd.setName(name);
		cmd.setText(text);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getGoal();
	}

	private void createProjectUser(String username) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
		cmd.setEditedBy(admin);
		cmd.setUsername(username);
		cmd.setPassword("spelling-test");
		cmd.setRepassword("spelling-test");
		cmd.setName(username);
		cmd.setEmailAddress(username + "@example.com");
		cmd.setPhoneNumber("");
		cmd.setOrganizationName("SpellingTestOrg");
		cmd.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(cmd);
	}

	private void addStakeholder(Project project, String username, Set<String> permissionKeys)
			throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditUserStakeholderCommand cmd = getProjectCommandFactory()
				.newEditUserStakeholderCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setUsername(username);
		cmd.setStakeholderPermissions(permissionKeys);
		getCommandHandler().execute(cmd);
	}
}
