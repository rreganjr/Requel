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
package com.rreganjr.requel.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * #445: a command from a signed-in caller whose account can't be resolved is refused; it never
 * runs as an internal (bootstrap) call. With no one signed in, commands run as before.
 */
class CurrentUserCommandHandlerTest {

	private final CommandHandler delegate = mock(CommandHandler.class);
	private final CurrentUserResolver resolver = mock(CurrentUserResolver.class);
	private final CurrentUserCommandHandler handler = new CurrentUserCommandHandler(delegate, resolver);
	private final EditCommand command = mock(EditCommand.class);

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	private static void signIn(String username) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				username, null, List.of(new SimpleGrantedAuthority("ROLE_ProjectUserRole"))));
	}

	@Test
	void anUnresolvableSignedInAccountIsRefusedBeforeTheCommandRuns() throws Exception {
		signIn("gone");
		when(resolver.resolve()).thenThrow(new RuntimeException("No user for username gone"));

		assertThatThrownBy(() -> handler.execute(command)).isInstanceOf(AuthorizationException.class);
		verify(delegate, never()).execute(any());
		verify(command, never()).setEditedBy(any());
	}

	@Test
	void aSignedInAccountBecomesEditedBy() throws Exception {
		signIn("alice");
		User alice = mock(User.class);
		when(resolver.resolve()).thenReturn(alice);

		handler.execute(command);

		verify(command).setEditedBy(alice);
		verify(delegate).execute(command);
	}

	@Test
	void withNoOneSignedInTheCommandRunsAsAnInternalCall() throws Exception {
		handler.execute(command);

		verify(resolver, never()).resolve();
		verify(command, never()).setEditedBy(any());
		verify(delegate).execute(command);
	}

	@Test
	void anAnonymousCallerIsTreatedAsNoOneSignedIn() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
				"key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

		handler.execute(command);

		verify(resolver, never()).resolve();
		verify(delegate).execute(command);
	}
}
