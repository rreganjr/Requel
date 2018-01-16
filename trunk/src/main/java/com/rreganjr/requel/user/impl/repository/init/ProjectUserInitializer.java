/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.user.impl.repository.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rreganjr.AbstractSystemInitializer;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.user.exception.NoSuchUserException;

/**
 * @author ron
 */
@Component("projectUserInitializer")
@Scope("prototype")
public class ProjectUserInitializer extends AbstractSystemInitializer {

	private final UserRepository userRepository;
	private final EditUserCommand command;
	private final CommandHandler commandHandler;

	/**
	 * @param userRepository
	 * @param commandHandler
	 * @param command
	 */
	@Autowired
	public ProjectUserInitializer(UserRepository userRepository, CommandHandler commandHandler,
			EditUserCommand command) {
		super(100);
		this.userRepository = userRepository;
		this.commandHandler = commandHandler;
		this.command = command;
	}

	@Override
	public void initialize() {
		try {
			userRepository.findUserByUsername("project");
		} catch (NoSuchUserException e) {
			try {
				command.setUsername("project");
				command.setPassword("project");
				command.setRepassword("project");
				command.setName("Builtin Project User");
				command.setEmailAddress("rreganjr@users.sourceforge.net");
				command.setOrganizationName("Requel");
				command.addUserRoleName(ProjectUserRole.getRoleName(ProjectUserRole.class));
				command.addUserRolePermissionName(ProjectUserRole
						.getRoleName(ProjectUserRole.class), ProjectUserRole.createProjects
						.getName());
				commandHandler.execute(command);
			} catch (Exception e2) {
				log.error("failed to initialize the project user: " + e2, e2);
			}
		}
	}
}
