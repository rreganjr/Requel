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
package com.rreganjr.requel.user.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.rreganjr.platform.identity.User;

/** #260: an assistant identity is recognised as one and can't log in. */
class AssistantUserRoleTest {

	@Test
	void anAssistantIdentityIsAnAssistantAndCannotLogIn() {
		UserImpl identity = new UserImpl("assistant-legacy-lexical", "Secret-pw-123", "Assistant",
				"a@requel.invalid", null, null, Boolean.FALSE);
		assertThat(identity.isPassword("Secret-pw-123")).isTrue();
		assertThat(User.isAssistant(identity)).isFalse();

		identity.grantRole(AssistantUserRole.class);

		assertThat(User.isAssistant(identity)).isTrue();
		assertThat(identity.isPassword("Secret-pw-123")).isFalse();
	}

	@Test
	void theOriginalAccountIsAnAssistantAndCannotLogInWithItsOldPassword() {
		UserImpl original = new UserImpl(User.ASSISTANT_USERNAME, "assistant", "Assistant",
				"assistant@requel.invalid", null, null, Boolean.FALSE);
		assertThat(User.isAssistant(original)).isTrue();
		assertThat(original.isPassword("assistant")).isFalse();
	}

	@Test
	void anIdentityUsernameIsThePrefixAndTheAssistantId() {
		assertThat(User.assistantUsername("ai-requirements-review"))
				.isEqualTo("assistant-ai-requirements-review");
	}
}
