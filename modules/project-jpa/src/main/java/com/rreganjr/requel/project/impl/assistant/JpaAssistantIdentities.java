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
package com.rreganjr.requel.project.impl.assistant;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.repository.jpa.EntityProxyInterceptor;
import com.rreganjr.requel.project.AssistantIdentities;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.impl.UserStakeholderImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import com.rreganjr.requel.user.impl.AssistantUserRole;
import com.rreganjr.requel.user.impl.UserImpl;

/**
 * Issue #260: finds or creates the user an assistant writes as ({@code assistant-<assistantId>},
 * holding {@link AssistantUserRole} and {@link ProjectUserRole}), and makes it a stakeholder of
 * the project with the assistant permission set the first time it writes there.
 *
 * <p>A new identity is created in its own transaction, so a concurrent run creating the same one
 * cannot poison the caller's: the loser reads the winner's row. Stakeholder rows are added in the
 * caller's transaction, which already holds the project lock (#279).
 */
@Component
public class JpaAssistantIdentities implements AssistantIdentities {

	private static final Logger log = LoggerFactory.getLogger(JpaAssistantIdentities.class);

	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	/** Lazy: the command handler chain reaches the assistant worker, which reaches this. */
	private final ObjectProvider<CommandHandler> commandHandler;
	private final ObjectProvider<EditUserCommand> editUserCommands;
	private final TransactionTemplate newTransaction;

	@Autowired
	public JpaAssistantIdentities(UserRepository userRepository,
			ProjectRepository projectRepository, ObjectProvider<CommandHandler> commandHandler,
			ObjectProvider<EditUserCommand> editUserCommands,
			PlatformTransactionManager transactionManager) {
		this.userRepository = userRepository;
		this.projectRepository = projectRepository;
		this.commandHandler = commandHandler;
		this.editUserCommands = editUserCommands;
		this.newTransaction = new TransactionTemplate(transactionManager);
		this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@Override
	public User identityFor(String assistantId, Long projectId) {
		User user = findOrCreate(assistantId);
		if (projectId != null) {
			ensureStakeholder(projectRepository.findById(Project.class, projectId), user);
		}
		return userRepository.get(user);
	}

	/** The identity for {@code assistantId}, created (in its own transaction) if missing. */
	public User findOrCreate(String assistantId) {
		String username = com.rreganjr.platform.identity.User.assistantUsername(assistantId);
		try {
			return userRepository.findUserByUsername(username);
		} catch (NoSuchUserException missing) {
			try {
				newTransaction.executeWithoutResult(status -> create(username, assistantId));
			} catch (RuntimeException e) {
				// Most likely a concurrent run created it first; the read below decides.
				log.debug("creating assistant identity {} failed: {}", username, e.toString());
			}
			return userRepository.findUserByUsername(username);
		}
	}

	private void create(String username, String assistantId) {
		EditUserCommand command = editUserCommands.getObject();
		// Never used: an assistant identity cannot log in (UserImpl.isPassword).
		String password = UUID.randomUUID().toString();
		command.setUsername(username);
		command.setPassword(password);
		command.setRepassword(password);
		command.setName("Assistant " + assistantId);
		command.setEmailAddress(username + "@requel.invalid");
		command.setOrganizationName("Requel");
		command.addUserRoleName(ProjectUserRole.class.getSimpleName());
		command.setEditable(Boolean.FALSE);
		try {
			commandHandler.getObject().execute(command);
		} catch (Exception e) {
			throw new IllegalStateException("could not create assistant identity " + username, e);
		}
		grantAssistantRole(userRepository.findUserByUsername(username));
		log.info("Created assistant identity {}", username);
	}

	/** Give {@code user} the assistant role if it lacks it. Call inside a transaction. */
	public void grantAssistantRole(User user) {
		if (EntityProxyInterceptor.unwrap(user) instanceof UserImpl impl
				&& !impl.hasRole(AssistantUserRole.class)) {
			impl.grantRole(AssistantUserRole.class);
			userRepository.merge(impl);
		}
	}

	private void ensureStakeholder(Project found, User identity) {
		// Repositories wrap what they return in an EntityProxy unless called from a command
		// (DomainObjectWrappingAdvice). A proxy is not the managed instance, so a stakeholder
		// holding one cascades its persist into what Hibernate sees as a detached user.
		Project project = EntityProxyInterceptor.unwrap(projectRepository.get(found));
		User user = EntityProxyInterceptor.unwrap(userRepository.get(identity));
		if (isStakeholder(project, user)) {
			return;
		}
		UserStakeholderImpl stakeholder = EntityProxyInterceptor.unwrap(projectRepository.persist(
				new UserStakeholderImpl(project, user, user)));
		for (StakeholderPermission permission : projectRepository
				.findAssistantStakeholderPermissions()) {
			stakeholder.grantStakeholderPermission(EntityProxyInterceptor.unwrap(permission));
		}
		project.getStakeholders().add(stakeholder);
		try {
			stakeholder.ensureProjectMembership();
		} catch (RuntimeException e) {
			log.warn("Assistant identity {} has no ProjectUserRole; skipping membership on {}",
					user.getUsername(), project.getName(), e);
		}
		log.info("Added assistant identity {} as a stakeholder of {}", user.getUsername(),
				project.getName());
	}

	private static boolean isStakeholder(Project project, User user) {
		try {
			UserStakeholder stakeholder = project.getUserStakeholder(user);
			return stakeholder != null;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
