/*
 * $Id: ProjectUserInitializer.java,v 1.9 2009/03/29 11:59:30 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user.impl.repository.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.AbstractSystemInitializer;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.ProjectUserRole;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.command.EditUserCommand;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;

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
				command.setEmailAddress("rreganjr@acm.org");
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
