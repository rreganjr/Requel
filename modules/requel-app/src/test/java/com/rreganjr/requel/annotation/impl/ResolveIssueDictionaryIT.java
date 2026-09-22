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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.command.EditAddWordToDictionaryPositionCommand;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditProjectDictionaryWordCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.validator.EntityValidationException;

/**
 * Authorization on the "Add to Dictionary" resolve and on the dictionary write underneath it
 * (issue #312).
 * <p>
 * #305 gated the resolve; #313 made the write project-scoped, which is what allows the write
 * itself to be gated against the project it touches. These cover the four things that changed:
 * the happy path still lands in the project's dictionary and nowhere else, a stakeholder without
 * {@code Annotation[Edit]} cannot use the resolver, the write command refuses on its own when it
 * is executed outside the resolver, and a command with no project writes nothing at all rather
 * than falling back to the installation-wide dictionary.
 *
 * @author ron
 */
public class ResolveIssueDictionaryIT extends AbstractIntegrationTestCase {

	/**
	 * The happy path, with the part that matters to #313 pinned: the word lands in the project's
	 * own dictionary, and the installation-wide one is left alone.
	 */
	@Test
	public void addsTheWordToTheProjectDictionaryAndNowhereElse() throws Exception {
		Project project = createProject("dict-auth-allowed");
		User admin = getUserRepository().findUserByUsername("admin");
		Goal goal = createGoal(project, "Use Requelish terminology");
		String word = invented("Requelish");

		resolveAddToDictionary(project, goal, word, admin);

		Issue resolved = getAnnotationRepository().findIssue(project, goal, issueText(word));
		assertTrue(resolved.isResolved(), "the lexical issue should be resolved");
		assertTrue(getDictionaryRepository().isKnownWord(project.getId(), word),
				"the word should be known in the project it was added to");
		assertFalse(getDictionaryRepository().isKnownWord(word),
				"the word must not reach the installation-wide dictionary");
	}

	/**
	 * A stakeholder without {@code Annotation[Edit]} cannot add a word through the resolver. The
	 * refusal comes from #305's gate on the resolve, before the dictionary command is built at
	 * all — {@link #theDictionaryWriteIsGatedOnItsOwn()} is what proves the write is not relying
	 * on that outer gate.
	 */
	@Test
	public void refusesTheResolveWithoutAnnotationEdit() throws Exception {
		Project project = createProject("dict-auth-refused");
		User admin = getUserRepository().findUserByUsername("admin");
		Goal goal = createGoal(project, "Use Requelese terminology");
		String word = invented("Requelese");

		User restricted = stakeholderWithoutAnnotationEdit(project, "dict-noannotation");

		LexicalIssue issue = newLexicalIssue(project, goal, word, admin);
		AddWordToDictionaryPosition position = newAddWordPosition(issue, word, admin);

		ResolveIssueCommand resolve = getAnnotationCommandFactory()
				.newResolveIssueCommand(position);
		resolve.setEditedBy(restricted);
		resolve.setIssue(issue);
		resolve.setPosition(position);
		resolve.setAnnotatable(goal);

		assertThrows(AuthorizationException.class, () -> getCommandHandler().execute(resolve),
				"Add to Dictionary must be refused without Annotation[Edit]");

		Issue stillOpen = getAnnotationRepository().findIssue(project, goal, issueText(word));
		assertFalse(stillOpen.isResolved(),
				"the issue must stay unresolved when the resolve is refused");
		assertFalse(getDictionaryRepository().isKnownWord(project.getId(), word),
				"a refused resolve must not add the word");
	}

	/**
	 * The point of #312. Executed on its own rather than through the resolver, the dictionary
	 * write is still checked: it is an {@code AuthorizableCommand} in its own right, requiring
	 * {@code Annotation[Edit]} on the project it writes to.
	 * <p>
	 * This is the test that fails if the command is ever re-parented onto a base class that
	 * implements {@code AuthorizationExemptable} — {@code AuthorizingCommandHandler} honours that
	 * flag before it reads the authorization requirement, so the gate would go quiet rather than
	 * loud.
	 */
	@Test
	public void theDictionaryWriteIsGatedOnItsOwn() throws Exception {
		Project project = createProject("dict-auth-direct");
		String word = invented("Requelian");

		User restricted = stakeholderWithoutAnnotationEdit(project, "dict-direct");

		EditProjectDictionaryWordCommand cmd = getProjectCommandFactory()
				.newEditDictionaryWordCommand();
		cmd.setEditedBy(restricted);
		cmd.setProject(project);
		cmd.setLemma(word);

		assertThrows(AuthorizationException.class, () -> getCommandHandler().execute(cmd),
				"the dictionary write must refuse without Annotation[Edit], resolver or not");
		assertFalse(getDictionaryRepository().isKnownWord(project.getId(), word),
				"a refused write must leave the project dictionary alone");
	}

	/**
	 * A command with no project is refused rather than writing installation-wide, which is what
	 * {@code setProjectId(null)} used to mean.
	 * <p>
	 * With a user set, the refusal is the stakeholder check: there is no project to be a
	 * stakeholder on. The validation in {@code execute()} is the second line of defence, for the
	 * bootstrap case below.
	 */
	@Test
	public void refusesAWriteWithNoProject() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		String word = invented("Requelless");

		EditProjectDictionaryWordCommand cmd = getProjectCommandFactory()
				.newEditDictionaryWordCommand();
		cmd.setEditedBy(admin);
		cmd.setLemma(word);

		assertThrows(AuthorizationException.class, () -> getCommandHandler().execute(cmd),
				"a write with no project must be refused");
		assertFalse(getDictionaryRepository().isKnownWord(word),
				"nothing may reach the installation-wide dictionary");
	}

	/**
	 * A project id on its own does not authorize anything. {@code setProjectId} exists because
	 * {@code EditDictionaryWordCommand} is declared in a module that cannot name a {@code Project}
	 * (#313); the authorization check needs the project itself, so a command given only an id is
	 * refused rather than trusted. Fail closed, and no silent installation-wide fallback.
	 */
	@Test
	public void refusesAWriteGivenOnlyAProjectId() throws Exception {
		Project project = createProject("dict-auth-id-only");
		User admin = getUserRepository().findUserByUsername("admin");
		String word = invented("Requelid");

		EditProjectDictionaryWordCommand cmd = getProjectCommandFactory()
				.newEditDictionaryWordCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectId(project.getId());
		cmd.setLemma(word);

		assertThrows(AuthorizationException.class, () -> getCommandHandler().execute(cmd),
				"an id without the project itself must not authorize a write");
		assertFalse(getDictionaryRepository().isKnownWord(project.getId(), word),
				"a refused write must leave the project dictionary alone");
	}

	/**
	 * The bootstrap escape does not become a hole. {@code AuthorizingCommandHandler} skips the
	 * check entirely when {@code editedBy} is null — the path initializers use — so without the
	 * validation in {@code execute()} an unauthenticated caller would write installation-wide
	 * exactly as before #312.
	 */
	@Test
	public void refusesAWriteWithNoProjectAndNoUser() throws Exception {
		String word = invented("Requelnull");

		EditProjectDictionaryWordCommand cmd = getProjectCommandFactory()
				.newEditDictionaryWordCommand();
		cmd.setLemma(word);

		assertThrows(EntityValidationException.class, () -> getCommandHandler().execute(cmd),
				"a write with no project and no user must fail validation");
		assertFalse(getDictionaryRepository().isKnownWord(word),
				"nothing may reach the installation-wide dictionary");
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * A distinct word per test. The Spring context and its H2 database are shared by every IT in
	 * the run, and a project dictionary write is idempotent by decision (#313), so a word reused
	 * across tests would make "was it added?" depend on test order.
	 */
	private static String invented(String stem) {
		return stem + Long.toString(System.nanoTime(), 36);
	}

	private static String issueText(String word) {
		return "Unknown word: " + word;
	}

	private void resolveAddToDictionary(Project project, Goal goal, String word, User editedBy)
			throws Exception {
		LexicalIssue issue = newLexicalIssue(project, goal, word, editedBy);
		AddWordToDictionaryPosition position = newAddWordPosition(issue, word, editedBy);

		ResolveIssueCommand resolve = getAnnotationCommandFactory()
				.newResolveIssueCommand(position);
		resolve.setEditedBy(editedBy);
		resolve.setIssue(issue);
		resolve.setPosition(position);
		resolve.setAnnotatable(goal);
		getCommandHandler().execute(resolve);
	}

	private LexicalIssue newLexicalIssue(Project project, Goal goal, String word, User editedBy)
			throws Exception {
		EditLexicalIssueCommand cmd = getAnnotationCommandFactory().newEditLexicalIssueCommand();
		cmd.setEditedBy(editedBy);
		cmd.setGroupingObject(project);
		cmd.setAnnotatable(goal);
		cmd.setText(issueText(word));
		cmd.setMustBeResolved(false);
		cmd.setWord(word);
		cmd.setAnnotatableEntityPropertyName("Text");
		cmd = getCommandHandler().execute(cmd);
		return (LexicalIssue) cmd.getIssue();
	}

	private AddWordToDictionaryPosition newAddWordPosition(LexicalIssue issue, String word,
			User editedBy) throws Exception {
		EditAddWordToDictionaryPositionCommand cmd = getAnnotationCommandFactory()
				.newEditAddWordToDictionaryPositionCommand();
		cmd.setEditedBy(editedBy);
		cmd.setIssue(issue);
		cmd.setText("Add '" + word + "' to the project dictionary");
		cmd = getCommandHandler().execute(cmd);
		return (AddWordToDictionaryPosition) cmd.getPosition();
	}

	/**
	 * A real stakeholder on the project holding Goal[Edit] and nothing else — enough to prove the
	 * refusal is about {@code Annotation[Edit]} rather than about not being a stakeholder at all.
	 */
	private User stakeholderWithoutAnnotationEdit(Project project, String prefix) throws Exception {
		String username = prefix + "-" + System.nanoTime();
		createProjectUser(username);
		addStakeholder(project, username, Set.of(StakeholderPermissionImpl
				.generatePermissionKey(Goal.class, StakeholderPermissionType.Edit)));
		return getUserRepository().findUserByUsername(username);
	}

	private Project createProject(String label) throws Exception {
		long ts = System.nanoTime();
		User admin = getUserRepository().findUserByUsername("admin");
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(admin);
		cmd.setName(label + "-" + ts);
		cmd.setText("test project for " + label);
		cmd.setOrganizationName("DictionaryAuthTestOrg-" + ts);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	private Goal createGoal(Project project, String name) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(admin);
		cmd.setGoalContainer(project);
		cmd.setName(name);
		cmd.setText("A goal for dictionary authorization tests.");
		cmd = getCommandHandler().execute(cmd);
		return cmd.getGoal();
	}

	private void createProjectUser(String username) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
		cmd.setEditedBy(admin);
		cmd.setUsername(username);
		cmd.setPassword("dictionary-test");
		cmd.setRepassword("dictionary-test");
		cmd.setName(username);
		cmd.setEmailAddress(username + "@example.com");
		cmd.setPhoneNumber("");
		cmd.setOrganizationName("DictionaryAuthTestOrg");
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
