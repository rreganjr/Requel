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
package com.rreganjr.requel.user.impl.repository.init;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rreganjr.platform.bootstrap.AbstractSystemInitializer;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinitionStore;
import com.rreganjr.requel.project.impl.assistant.JpaAssistantIdentities;

/**
 * Create the assistant user if it doesn't exist. The assistant user is a
 * psuedo-user that represents assistants that do analysis of project entities.
 *
 * <p>Issue #260: each assistant now writes as its own identity. This also gives the original
 * user the assistant role (so its annotations stay machine-authored and it can't log in), and
 * creates the identity of every bean assistant and bundled definition ahead of time, so runs
 * started together on a fresh install don't race to create them.
 * 
 * @author ron
 */
@Component("assistantUserInitializer")
@Scope("prototype")
public class AssistantUserInitializer extends AbstractSystemInitializer {

	private final UserRepository userRepository;
	private final EditUserCommand command;
	private final CommandHandler commandHandler;
	private JpaAssistantIdentities identities;
	private List<RequelAssistant<?>> assistants = List.of();
	private AssistantDefinitionStore definitionStore;
	private TransactionTemplate transactionTemplate;

	/**
	 * @param userRepository
	 * @param commandHandler
	 * @param command
	 */
	@Autowired
	public AssistantUserInitializer(UserRepository userRepository, CommandHandler commandHandler,
			EditUserCommand command) {
		super(101);
		this.userRepository = userRepository;
		this.commandHandler = commandHandler;
		this.command = command;
	}

	@Autowired(required = false)
	public void setIdentities(JpaAssistantIdentities identities) {
		this.identities = identities;
	}

	@Autowired(required = false)
	public void setAssistants(List<RequelAssistant<?>> assistants) {
		this.assistants = assistants == null ? List.of() : List.copyOf(assistants);
	}

	@Autowired(required = false)
	public void setDefinitionStore(AssistantDefinitionStore definitionStore) {
		this.definitionStore = definitionStore;
	}

	@Autowired(required = false)
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Override
	public void initialize() {
		try {
			userRepository.findUserByUsername(User.ASSISTANT_USERNAME);
		} catch (NoSuchUserException e) {
			try {
				command.setUsername(User.ASSISTANT_USERNAME);
				// A password is required, but an assistant never logs in (#260: UserImpl.isPassword
				// refuses assistant identities), so it is random rather than the old "assistant".
				String password = java.util.UUID.randomUUID().toString();
				command.setPassword(password);
				command.setRepassword(password);
				command.setName("Analysis Assistant");
				command.setEmailAddress("assistant@requel.invalid");
				command.setOrganizationName("Requel");
				command.addUserRoleName(SystemAdminUserRole.getRoleName(ProjectUserRole.class));
				command.setEditable(Boolean.FALSE);
				commandHandler.execute(command);
			} catch (Exception e2) {
				log.error("failed to initialize the assistant user: " + e2, e2);
			}
		}
		ensureIdentities();
	}

	/** Issue #260: the role on the original user, and one identity per known assistant. */
	private void ensureIdentities() {
		if (identities == null || transactionTemplate == null) {
			return;
		}
		try {
			transactionTemplate.executeWithoutResult(status -> identities.grantAssistantRole(
					userRepository.findUserByUsername(User.ASSISTANT_USERNAME)));
		} catch (RuntimeException e) {
			log.error("failed to give the assistant user the assistant role: " + e, e);
		}
		Set<String> ids = new LinkedHashSet<String>();
		for (RequelAssistant<?> assistant : assistants) {
			ids.add(assistant.assistantId());
		}
		if (definitionStore != null) {
			for (AssistantDefinition definition : definitionStore.bundled()) {
				ids.add(definition.key());
			}
		}
		for (String id : ids) {
			try {
				identities.findOrCreate(id);
			} catch (RuntimeException e) {
				log.error("failed to create the identity for assistant " + id + ": " + e, e);
			}
		}
	}
}
