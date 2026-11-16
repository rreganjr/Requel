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
package com.rreganjr.requel.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.user.impl.repository.init.DefaultAdminPasswordInitializer;

/**
 * Issue #381: the startup check flags an account whose password is still "admin" and leaves a
 * changed one alone. It runs against throwaway accounts here (the test profile turns the real
 * check off, and the context's admin must stay usable for every other IT).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DefaultAdminPasswordInitializerIT extends AbstractIntegrationTestCase {

	@Autowired
	private PlatformTransactionManager transactionManager;

	private int counter;

	@BeforeAll
	void setUp() throws Exception {
		initializeBaselineData();
	}

	@Test
	void theDefaultPasswordIsFlagged() throws Exception {
		String username = newUser("admin");
		run(new DefaultAdminPasswordInitializer(getUserRepository(), true, username));
		assertThat(getUserRepository().findUserByUsername(username).isMustChangePassword()).isTrue();
	}

	@Test
	void aChangedPasswordIsLeftAlone() throws Exception {
		String username = newUser("changed-already");
		run(new DefaultAdminPasswordInitializer(getUserRepository(), true, username));
		assertThat(getUserRepository().findUserByUsername(username).isMustChangePassword()).isFalse();
	}

	@Test
	void turnedOffItDoesNothing() throws Exception {
		String username = newUser("admin");
		run(new DefaultAdminPasswordInitializer(getUserRepository(), false, username));
		assertThat(getUserRepository().findUserByUsername(username).isMustChangePassword()).isFalse();
	}

	@Test
	void aMissingAccountIsIgnored() {
		run(new DefaultAdminPasswordInitializer(getUserRepository(), true, "no-such-user-381"));
	}

	@Test
	void theTestProfileLeavesTheRealAdminUnflagged() {
		assertThat(getUserRepository().findUserByUsername("admin").isMustChangePassword()).isFalse();
	}

	private void run(DefaultAdminPasswordInitializer initializer) {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> initializer.initialize());
	}

	private String newUser(String password) throws Exception {
		String username = "dapi-" + System.currentTimeMillis() + "-" + (++counter);
		User admin = getUserRepository().findUserByUsername("admin");
		EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
		cmd.setEditedBy(admin);
		cmd.setUsername(username);
		cmd.setPassword(password);
		cmd.setRepassword(password);
		cmd.setName(username);
		cmd.setEmailAddress(username + "@example.com");
		cmd.setPhoneNumber("");
		cmd.setOrganizationName("DapiTestOrg");
		cmd.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(cmd);
		return username;
	}
}
