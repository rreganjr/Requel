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
 * itself to be gated against the project it touches. These cover what changed: the happy path
 * still lands in the project's dictionary and nowhere else, and every route to the write is
 * refused without {@code Annotation[Edit]} on that project — through the resolver, through the
 * command on its own, and with a project id that has no project object behind it.
 * <p>
 * <b>Two projects, deliberately.</b> Every IT in the run shares one H2 database and one Spring
 * context, and a project created here stays in the {@code admin} user's active projects for the
 * rest of the run. {@code AddStoryToStoryContainer}'s {@code em.refresh} loads a graph with six
 * eager {@code user -> user_roles -> active_projects -> pods} branches, so its row count
 * multiplies with that count rather than adding to it; four projects from this class were enough
 * to take {@code CommandGatewayIT} from slow to out of memory on CI. Assertions are grouped so
 * that the fixture stays at two projects until that fetch graph is fixed.
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

		LexicalIssue issue = newLexicalIssue(project, goal, word, admin);
		AddWordToDictionaryPosition position = newAddWordPosition(issue, word, admin);
		getCommandHandler().execute(resolveCommand(issue, position, goal, admin));

		Issue resolved = getAnnotationRepository().findIssue(project, goal, issueText(word));
		assertTrue(resolved.isResolved(), "the lexical issue should be resolved");
		assertTrue(getDictionaryRepository().isKnownWord(project.getId(), word),
				"the word should be known in the project it was added to");
		assertFalse(getDictionaryRepository().isKnownWord(word),
				"the word must not reach the installation-wide dictionary");
	}

	/**
	 * Every route to the write, refused, on one fixture.
	 * <p>
	 * The first is #305's gate on the resolve. The second is the point of #312: executed on its
	 * own rather than through the resolver, the write is still checked, because it is an
	 * {@code AuthorizableCommand} in its own right. That second assertion is what fails if the
	 * command is ever re-parented onto a base class implementing {@code AuthorizationExemptable} —
	 * {@code AuthorizingCommandHandler} honours that flag before it reads the authorization
	 * requirement, so the gate would go quiet rather than loud.
	 * <p>
	 * The third runs as {@code admin}, who does hold {@code Annotation[Edit]} here: a project id
	 * with no project object behind it authorizes nothing, so the refusal is about the missing
	 * project rather than a missing permission. {@code setProjectId} exists only because
	 * {@code EditDictionaryWordCommand} is declared in a module that cannot name a {@code Project}
	 * (#313). Fail closed, and no silent installation-wide fallback.
	 */
	@Test
	public void refusesEveryRouteToTheWriteWithoutAnnotationEdit() throws Exception {
		Project project = createProject("dict-auth-refused");
		User admin = getUserRepository().findUserByUsername("admin");
		Goal goal = createGoal(project, "Use Requelese terminology");
		User restricted = stakeholderWithoutAnnotationEdit(project, "dict-noannotation");

		// 1 — through the resolver
		String resolverWord = invented("Requelese");
		LexicalIssue issue = newLexicalIssue(project, goal, resolverWord, admin);
		AddWordToDictionaryPosition position = newAddWordPosition(issue, resolverWord, admin);
		ResolveIssueCommand resolve = resolveCommand(issue, position, goal, restricted);

		assertThrows(AuthorizationException.class, () -> getCommandHandler().execute(resolve),
				"Add to Dictionary must be refused without Annotation[Edit]");
		Issue stillOpen = getAnnotationRepository().findIssue(project, goal, issueText(resolverWord));
		assertFalse(stillOpen.isResolved(),
				"the issue must stay unresolved when the resolve is refused");
		assertFalse(getDictionaryRepository().isKnownWord(project.getId(), resolverWord),
				"a refused resolve must not add the word");

		// 2 — the write command on its own, outside the resolver
		String directWord = invented("Requelian");
		EditProjectDictionaryWordCommand direct = getProjectCommandFactory()
				.newEditDictionaryWordCommand();
		direct.setEditedBy(restricted);
		direct.setProject(project);
		direct.setLemma(directWord);

		assertThrows(AuthorizationException.class, () -> getCommandHandler().execute(direct),
				"the dictionary write must refuse without Annotation[Edit], resolver or not");
		assertFalse(getDictionaryRepository().isKnownWord(project.getId(), directWord),
				"a refused write must leave the project dictionary alone");

		// 3 — a project id with no project behind it, as a user who does hold the permission
		String idOnlyWord = invented("Requelid");
		EditProjectDictionaryWordCommand idOnly = getProjectCommandFactory()
				.newEditDictionaryWordCommand();
		idOnly.setEditedBy(admin);
		idOnly.setProjectId(project.getId());
		idOnly.setLemma(idOnlyWord);

		assertThrows(AuthorizationException.class, () -> getCommandHandler().execute(idOnly),
				"an id without the project itself must not authorize a write");
		assertFalse(getDictionaryRepository().isKnownWord(project.getId(), idOnlyWord),
				"a refused write must leave the project dictionary alone");
	}

	/**
	 * A command with no project writes nothing, which is what {@code setProjectId(null)} used to
	 * mean — the installation-wide dictionary.
	 * <p>
	 * With a user set the refusal is the stakeholder check: there is no project to be a
	 * stakeholder on. Without one it is the validation in {@code execute()}, and that second half
	 * is the one worth having: {@code AuthorizingCommandHandler} skips the check entirely when
	 * {@code editedBy} is null, the path initializers use, so without the validation an
	 * unauthenticated caller would write installation-wide exactly as before #312.
	 * <p>
	 * Creates no project: see the class note.
	 */
	@Test
	public void refusesAWriteWithNoProject() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");

		String userWord = invented("Requelless");
		EditProjectDictionaryWordCommand withUser = getProjectCommandFactory()
				.newEditDictionaryWordCommand();
		withUser.setEditedBy(admin);
		withUser.setLemma(userWord);

		assertThrows(AuthorizationException.class, () -> getCommandHandler().execute(withUser),
				"a write with no project must be refused");
		assertFalse(getDictionaryRepository().isKnownWord(userWord),
				"nothing may reach the installation-wide dictionary");

		String bootstrapWord = invented("Requelnull");
		EditProjectDictionaryWordCommand withoutUser = getProjectCommandFactory()
				.newEditDictionaryWordCommand();
		withoutUser.setLemma(bootstrapWord);

		assertThrows(EntityValidationException.class,
				() -> getCommandHandler().execute(withoutUser),
				"a write with no project and no user must fail validation");
		assertFalse(getDictionaryRepository().isKnownWord(bootstrapWord),
				"nothing may reach the installation-wide dictionary");
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * A distinct word per assertion. The Spring context and its H2 database are shared by every IT
	 * in the run, and a project dictionary write is idempotent by decision (#313), so a word reused
	 * across tests would make "was it added?" depend on test order.
	 */
	private static String invented(String stem) {
		return stem + Long.toString(System.nanoTime(), 36);
	}

	private static String issueText(String word) {
		return "Unknown word: " + word;
	}

	private ResolveIssueCommand resolveCommand(LexicalIssue issue,
			AddWordToDictionaryPosition position, Goal goal, User editedBy) throws Exception {
		ResolveIssueCommand resolve = getAnnotationCommandFactory()
				.newResolveIssueCommand(position);
		resolve.setEditedBy(editedBy);
		resolve.setIssue(issue);
		resolve.setPosition(position);
		resolve.setAnnotatable(goal);
		return resolve;
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
