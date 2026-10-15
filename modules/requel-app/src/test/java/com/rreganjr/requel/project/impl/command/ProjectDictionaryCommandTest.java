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
package com.rreganjr.requel.project.impl.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.nlp.dictionary.ProjectDictionaryWord;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.command.AddProjectDictionaryWordCommand;
import com.rreganjr.requel.project.command.DeleteProjectDictionaryWordCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.validator.EntityValidationException;

/**
 * The project Dictionary page's commands (issue #319): {@code AddProjectDictionaryWord} and
 * {@code DeleteProjectDictionaryWord}, gated on {@code Project[Edit]}.
 * <p>
 * Every fixture is made here with a unique name; the Spring context is shared.
 *
 * @author ron
 */
public class ProjectDictionaryCommandTest extends AbstractIntegrationTestCase {

	@Test
	public void aProjectEditorAddsAWord() throws Exception {
		User admin = admin();
		Project project = createProject("pdc-add");

		AddProjectDictionaryWordCommand cmd = add(admin, project, "  requelcmdword  ");

		ProjectDictionaryWord word = cmd.getWord();
		assertNotNull(word, "the command returns the added word");
		assertNotNull(word.getId());
		assertEquals("requelcmdword", word.getLemma(), "the word is stored trimmed");
		assertTrue(getDictionaryRepository().isKnownWord(project.getId(), "requelcmdword"));
		assertEquals(1, getDictionaryRepository().countProjectWords(project.getId()));
	}

	@Test
	public void addingTheSameWordInAnotherCaseReturnsTheExistingRow() throws Exception {
		User admin = admin();
		Project project = createProject("pdc-idem");

		Long first = add(admin, project, "Requelidem").getWord().getId();
		Long second = add(admin, project, "REQUELIDEM").getWord().getId();

		assertEquals(first, second, "a repeat add returns the existing word");
		assertEquals(1, getDictionaryRepository().countProjectWords(project.getId()));
		assertEquals("Requelidem",
				getDictionaryRepository().findProjectWords(project.getId()).get(0).getLemma(),
				"the case first entered is kept");
	}

	@Test
	public void addIsRefusedWithoutProjectEdit() throws Exception {
		Project project = createProject("pdc-noedit");
		// Annotation[Edit] is enough to resolve "Add to Dictionary", not to manage the list.
		User restricted = createStakeholderUser(project, "pdc-noedit",
				Set.of(StakeholderPermissionImpl.generatePermissionKey(Annotation.class,
						StakeholderPermissionType.Edit)));

		AuthorizationException e = assertThrows(AuthorizationException.class,
				() -> add(restricted, reload(project), "requelrefused"));
		assertTrue(e.getMessage().contains("Project[Edit]"),
				"refused for the permission, not for membership: " + e.getMessage());
		assertEquals(0, getDictionaryRepository().countProjectWords(project.getId()));
	}

	@Test
	public void theAssistantCannotManageTheWordList() throws Exception {
		Project project = createProject("pdc-assistant");
		User assistant = getUserRepository().findUserByUsername("assistant");

		AuthorizationException e = assertThrows(AuthorizationException.class,
				() -> add(assistant, reload(project), "requelassistant"));
		assertTrue(e.getMessage().contains("Project[Edit]"),
				"the assistant is a stakeholder; it lacks the permission: " + e.getMessage());
		assertEquals(0, getDictionaryRepository().countProjectWords(project.getId()));
	}

	@Test
	public void aStakeholderWithProjectEditCanAddAndRemove() throws Exception {
		Project project = createProject("pdc-editor");
		User editor = createStakeholderUser(project, "pdc-editor",
				Set.of(StakeholderPermissionImpl.generatePermissionKey(Project.class,
						StakeholderPermissionType.Edit)));

		Long id = add(editor, reload(project), "requeleditor").getWord().getId();
		delete(editor, reload(project), id);

		assertEquals(0, getDictionaryRepository().countProjectWords(project.getId()));
	}

	@Test
	public void invalidWordsAreRefusedOnLemma() throws Exception {
		User admin = admin();
		Project project = createProject("pdc-invalid");

		for (String bad : new String[] { "", "   ", "two words", "tab\tword", "x".repeat(81) }) {
			EntityValidationException e = assertThrows(EntityValidationException.class,
					() -> add(admin, project, bad), "'" + bad + "' should be refused");
			assertTrue(e.getMessage().contains("word"),
					"the refusal should explain the word rule, got: " + e.getMessage());
		}
		assertNotNull(add(admin, project, "y".repeat(80)).getWord(),
				"an 80-character word is at the limit and accepted");
		assertEquals(1, getDictionaryRepository().countProjectWords(project.getId()));
	}

	@Test
	public void deleteRemovesTheWordById() throws Exception {
		User admin = admin();
		Project project = createProject("pdc-delete");
		Long id = add(admin, project, "requeldoomed").getWord().getId();
		add(admin, project, "requelkept");

		delete(admin, project, id);

		assertFalse(getDictionaryRepository().isKnownWord(project.getId(), "requeldoomed"));
		assertTrue(getDictionaryRepository().isKnownWord(project.getId(), "requelkept"));
		assertEquals(1, getDictionaryRepository().countProjectWords(project.getId()));
	}

	@Test
	public void deletingAnotherProjectsWordIsNotFoundAndChangesNothing() throws Exception {
		User admin = admin();
		Project projectA = createProject("pdc-cross-a");
		Project projectB = createProject("pdc-cross-b");
		Long idInB = add(admin, projectB, "requelcross").getWord().getId();

		assertThrows(NoSuchEntityException.class, () -> delete(admin, projectA, idInB));

		assertTrue(getDictionaryRepository().isKnownWord(projectB.getId(), "requelcross"),
				"project B's word must survive a delete through project A");
		assertEquals(1, getDictionaryRepository().countProjectWords(projectB.getId()));
	}

	@Test
	public void deleteIsRefusedWithoutProjectEdit() throws Exception {
		User admin = admin();
		Project project = createProject("pdc-delete-noedit");
		Long id = add(admin, project, "requelguarded").getWord().getId();
		User restricted = createStakeholderUser(project, "pdc-delete-noedit",
				Set.of(StakeholderPermissionImpl.generatePermissionKey(Annotation.class,
						StakeholderPermissionType.Edit)));

		AuthorizationException e = assertThrows(AuthorizationException.class,
				() -> delete(restricted, reload(project), id));
		assertTrue(e.getMessage().contains("Project[Edit]"),
				"refused for the permission, not for membership: " + e.getMessage());
		assertEquals(1, getDictionaryRepository().countProjectWords(project.getId()));
	}

	private AddProjectDictionaryWordCommand add(User user, Project project, String lemma)
			throws Exception {
		AddProjectDictionaryWordCommand cmd = getProjectCommandFactory()
				.newAddProjectDictionaryWordCommand();
		cmd.setEditedBy(user);
		cmd.setProject(project);
		cmd.setLemma(lemma);
		return getCommandHandler().execute(cmd);
	}

	private void delete(User user, Project project, Long wordId) throws Exception {
		DeleteProjectDictionaryWordCommand cmd = getProjectCommandFactory()
				.newDeleteProjectDictionaryWordCommand();
		cmd.setEditedBy(user);
		cmd.setProject(project);
		cmd.setWordId(wordId);
		getCommandHandler().execute(cmd);
	}

	private User admin() {
		return getUserRepository().findUserByUsername("admin");
	}

	private Project createProject(String label) throws Exception {
		long ts = System.nanoTime();
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(admin());
		cmd.setName(label + "-" + ts);
		cmd.setText("test project for " + label);
		cmd.setOrganizationName("ProjectDictionaryCommandTestOrg-" + ts);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	private User createStakeholderUser(Project project, String label, Set<String> permissionKeys)
			throws Exception {
		String username = label + "-" + System.nanoTime();
		EditUserCommand userCmd = getUserCommandFactory().newEditUserCommand();
		userCmd.setEditedBy(admin());
		userCmd.setUsername(username);
		userCmd.setPassword("dictionary-test");
		userCmd.setRepassword("dictionary-test");
		userCmd.setName(username);
		userCmd.setEmailAddress(username + "@example.com");
		userCmd.setPhoneNumber("");
		userCmd.setOrganizationName("ProjectDictionaryCommandTestOrg");
		userCmd.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(userCmd);

		EditUserStakeholderCommand stakeholderCmd = getProjectCommandFactory()
				.newEditUserStakeholderCommand();
		stakeholderCmd.setEditedBy(admin());
		stakeholderCmd.setProjectOrDomain(project);
		stakeholderCmd.setUsername(username);
		stakeholderCmd.setStakeholderPermissions(permissionKeys);
		getCommandHandler().execute(stakeholderCmd);
		return getUserRepository().findUserByUsername(username);
	}

	/**
	 * The project as a request would resolve it. The instance {@link #createProject} returned
	 * holds the stakeholder set from before {@link #createStakeholderUser} ran.
	 */
	private Project reload(Project project) {
		return getProjectRepository().get(project);
	}
}
