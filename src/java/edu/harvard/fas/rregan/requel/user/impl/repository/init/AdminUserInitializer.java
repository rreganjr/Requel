/*
 * $Id: AdminUserInitializer.java,v 1.9 2009/03/29 11:59:30 rregan Exp $
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
import edu.harvard.fas.rregan.requel.user.SystemAdminUserRole;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.command.EditUserCommand;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;

/**
 * Create the admin user if it doesn't exist.
 * 
 * @author ron
 */
@Component("adminUserInitializer")
@Scope("prototype")
public class AdminUserInitializer extends AbstractSystemInitializer {

	private final UserRepository userRepository;
	private final EditUserCommand command;
	private final CommandHandler commandHandler;

	/**
	 * @param userRepository
	 * @param commandHandler
	 * @param command
	 */
	@Autowired
	public AdminUserInitializer(UserRepository userRepository, CommandHandler commandHandler,
			EditUserCommand command) {
		super(100);
		this.userRepository = userRepository;
		this.commandHandler = commandHandler;
		this.command = command;
	}

	@Override
	public void initialize() {
		try {
			userRepository.findUserByUsername("admin");
		} catch (NoSuchUserException e) {
			try {
				command.setUsername("admin");
				command.setPassword("admin");
				command.setRepassword("admin");
				command.setName("System Administrator");
				command.setEmailAddress("rreganjr@users.sourceforge.net");
				command.setOrganizationName("Requel");
				command.addUserRoleName(SystemAdminUserRole.getRoleName(SystemAdminUserRole.class));
				commandHandler.execute(command);
			} catch (Exception e2) {
				log.error("failed to initialize the admin user: " + e2, e2);
			}
		}
	}
}
