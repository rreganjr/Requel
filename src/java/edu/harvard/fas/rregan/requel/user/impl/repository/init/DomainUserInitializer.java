/*
 * $Id: DomainUserInitializer.java,v 1.8 2009/03/29 11:59:30 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user.impl.repository.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.AbstractSystemInitializer;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.DomainAdminUserRole;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.command.EditUserCommand;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;

@Component("domainUserInitializer")
@Scope("prototype")
public class DomainUserInitializer extends AbstractSystemInitializer {

	private final UserRepository userRepository;
	private final EditUserCommand command;
	private final CommandHandler commandHandler;

	@Autowired
	public DomainUserInitializer(UserRepository userRepository, CommandHandler commandHandler,
			EditUserCommand command) {
		super(100);
		this.userRepository = userRepository;
		this.commandHandler = commandHandler;
		this.command = command;
	}

	@Override
	public void initialize() {
		try {
			// TODO: the domain admin user role has been disabled so the domain
			// user is disabled as well
			// userRepository.findUserByUsername("domain");
		} catch (NoSuchUserException e) {
			try {
				command.setUsername("domain");
				command.setPassword("domain");
				command.setRepassword("domain");
				command.setEmailAddress("rreganjr@acm.org");
				command.setOrganizationName("Requel");
				command.addUserRoleName(DomainAdminUserRole.getRoleName(DomainAdminUserRole.class));
				commandHandler.execute(command);
			} catch (Exception e2) {
				log.error("failed to initialize the domain user: " + e2, e2);
			}
		}
	}
}
