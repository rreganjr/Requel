/*
 * $Id: AssistantUserInitializer.java,v 1.9 2009/03/29 11:59:30 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
 * Elicitation System.
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
				command.setEmailAddress("rreganjr@users.sourceforge.net");
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
