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

import org.junit.jupiter.api.Test;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.nlp.dictionary.InstallDictionaryWord;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.project.command.AddInstallDictionaryWordCommand;
import com.rreganjr.requel.project.command.DeleteInstallDictionaryWordCommand;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.validator.EntityValidationException;

/**
 * The installation dictionary's commands (issue #319): administrators only.
 * <p>
 * An installation word is visible to every test in the shared context, so each test removes the
 * words it adds.
 *
 * @author ron
 */
public class InstallDictionaryCommandTest extends AbstractIntegrationTestCase {

	@Test
	public void anAdministratorAddsAndRemovesAWord() throws Exception {
		User admin = admin();

		InstallDictionaryWord word = add(admin, " requelinstallcmd ").getWord();
		try {
			assertNotNull(word);
			assertEquals("requelinstallcmd", word.getLemma(), "the word is stored trimmed");
			assertEquals(admin.getId(), word.getCreatedById(), "the adder is recorded");
			assertTrue(getDictionaryRepository().isKnownWord("requelinstallcmd"));
		} finally {
			delete(admin, word.getId());
		}
		assertFalse(getDictionaryRepository().isKnownWord("requelinstallcmd"));
		assertThrows(NoSuchEntityException.class, () -> delete(admin, word.getId()),
				"a second delete of the same id is not found");
	}

	@Test
	public void aNonAdministratorCannotAdd() throws Exception {
		User user = createProjectUser("idc-add");

		assertThrows(AuthorizationException.class, () -> add(user, "requelnotadmin"));
		assertFalse(getDictionaryRepository().isKnownWord("requelnotadmin"));
	}

	@Test
	public void aNonAdministratorCannotDelete() throws Exception {
		User admin = admin();
		User user = createProjectUser("idc-delete");
		InstallDictionaryWord word = add(admin, "requelprotected").getWord();
		try {
			assertThrows(AuthorizationException.class, () -> delete(user, word.getId()));
			assertTrue(getDictionaryRepository().isKnownWord("requelprotected"),
					"the word survives a refused delete");
		} finally {
			delete(admin, word.getId());
		}
	}

	@Test
	public void theWordRuleAppliesToInstallationWordsToo() throws Exception {
		User admin = admin();
		assertThrows(EntityValidationException.class, () -> add(admin, "two words"));
		assertThrows(EntityValidationException.class, () -> add(admin, " "));
	}

	private AddInstallDictionaryWordCommand add(User user, String lemma) throws Exception {
		AddInstallDictionaryWordCommand cmd = getProjectCommandFactory()
				.newAddInstallDictionaryWordCommand();
		cmd.setEditedBy(user);
		cmd.setLemma(lemma);
		return getCommandHandler().execute(cmd);
	}

	private void delete(User user, Long wordId) throws Exception {
		DeleteInstallDictionaryWordCommand cmd = getProjectCommandFactory()
				.newDeleteInstallDictionaryWordCommand();
		cmd.setEditedBy(user);
		cmd.setWordId(wordId);
		getCommandHandler().execute(cmd);
	}

	private User admin() {
		return getUserRepository().findUserByUsername("admin");
	}

	private User createProjectUser(String label) throws Exception {
		String username = label + "-" + System.nanoTime();
		EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
		cmd.setEditedBy(admin());
		cmd.setUsername(username);
		cmd.setPassword("dictionary-test");
		cmd.setRepassword("dictionary-test");
		cmd.setName(username);
		cmd.setEmailAddress(username + "@example.com");
		cmd.setPhoneNumber("");
		cmd.setOrganizationName("InstallDictionaryCommandTestOrg");
		cmd.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(cmd);
		return getUserRepository().findUserByUsername(username);
	}
}
