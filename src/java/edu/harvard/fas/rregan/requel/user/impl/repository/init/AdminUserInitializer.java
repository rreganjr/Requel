/*
 * $Id: AdminUserInitializer.java,v 1.9 2009/03/29 11:59:30 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
