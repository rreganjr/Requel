/*
 * $Id: AssistantUserInitializer.java,v 1.9 2009/03/29 11:59:30 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user.impl.repository.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.AbstractSystemInitializer;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.ProjectUserRole;
import edu.harvard.fas.rregan.requel.user.SystemAdminUserRole;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.command.EditUserCommand;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;

/**
 * Create the assistant user if it doesn't exist. The assistant user is a
 * psuedo-user that represents assistants that do analysis of project entities.
 * 
 * @author ron
 */
@Component("assistantUserInitializer")
@Scope("prototype")
public class AssistantUserInitializer extends AbstractSystemInitializer {

	private final UserRepository userRepository;
	private final EditUserCommand command;
	private final CommandHandler commandHandler;

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

	@Override
	public void initialize() {
		try {
			userRepository.findUserByUsername("assistant");
		} catch (NoSuchUserException e) {
			try {
				command.setUsername("assistant");
				// TODO: this user shouldn't be able to login, but a password is
				// required
				command.setPassword("assistant");
				command.setRepassword("assistant");
				command.setName("Analysis Assistant");
				command.setEmailAddress("rreganjr@acm.org");
				command.setOrganizationName("Requel");
				command.addUserRoleName(SystemAdminUserRole.getRoleName(ProjectUserRole.class));
				command.setEditable(Boolean.FALSE);
				commandHandler.execute(command);
			} catch (Exception e2) {
				log.error("failed to initialize the assistant user: " + e2, e2);
			}
		}
	}
}
