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
package com.rreganjr.requel.user.impl.repository.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rreganjr.platform.bootstrap.AbstractSystemInitializer;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import com.rreganjr.requel.user.Organization;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Grants the built-in admin user ProjectUserRole + createProjects so that
 * the admin can create and manage projects without a manual setup step.
 *
 * Runs at order 200, after AdminUserInitializer (order 100) has ensured
 * the admin account exists.
 */
@Component("adminProjectRoleInitializer")
@Scope("prototype")
public class AdminProjectRoleInitializer extends AbstractSystemInitializer {

	private final UserRepository userRepository;
	private final EditUserCommand command;
	private final CommandHandler commandHandler;

	@Autowired
	public AdminProjectRoleInitializer(UserRepository userRepository, CommandHandler commandHandler,
			EditUserCommand command) {
		super(200);
		this.userRepository = userRepository;
		this.commandHandler = commandHandler;
		this.command = command;
	}

	@Override
	public void initialize() {
		try {
			User admin = userRepository.findUserByUsername("admin");
			if (admin.hasRole(ProjectUserRole.class)) {
				return; // already granted — idempotent
			}
			// EditUser with editedBy = admin (self-edit as system admin) enables role updates.
			// isAdmin=true in EditUserCommandImpl.execute() because admin has SystemAdminUserRole.
			command.setUser(admin);
			command.setEditedBy(admin);
			command.setUsername(admin.getUsername());
			command.setName(admin.getName());
			command.setEmailAddress(admin.getEmailAddress());
			Organization org = admin.getOrganization();
			command.setOrganizationName(org != null ? org.getName() : null);
			// Provide the complete role set so updateRoles() doesn't revoke existing roles
			command.addUserRoleName(SystemAdminUserRole.getRoleName(SystemAdminUserRole.class));
			command.addUserRoleName(ProjectUserRole.getRoleName(ProjectUserRole.class));
			command.addUserRolePermissionName(ProjectUserRole.getRoleName(ProjectUserRole.class),
					ProjectUserRole.createProjects.getName());
			commandHandler.execute(command);
		} catch (Exception e) {
			log.error("failed to grant admin ProjectUserRole: " + e, e);
		}
	}
}
