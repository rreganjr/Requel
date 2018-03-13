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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.AbstractSystemInitializer;
import com.rreganjr.repository.EntityException;
import com.rreganjr.requel.user.AbstractUserRole;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.UserRole;
import com.rreganjr.requel.user.UserRolePermission;

/**
 * @author ron
 */
@Component("userRolePermissionsInitializer")
@Scope("prototype")
public class UserRolePermissionsInitializer extends AbstractSystemInitializer {

	private final UserRepository userRepository;

	@Autowired
	public UserRolePermissionsInitializer(UserRepository userRepository) {
		super(10);
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void initialize() {
		log.debug("update role permissions...");
		for (Class<? extends UserRole> userRoleType : userRepository.findUserRoleTypes()) {
			Set<UserRolePermission> fixedPermissions = new HashSet<UserRolePermission>();
			for (UserRolePermission permission : AbstractUserRole
					.getAvailableUserRolePermissions(userRoleType)) {
				try {
					permission = userRepository.findUserRolePermission(userRoleType, permission
							.getName());
					log.debug(permission + " is already persistent.");
				} catch (EntityException e) {
					log.debug("creating: " + permission);
					permission = userRepository.persist(permission);
				}
				fixedPermissions.add(permission);
			}
			AbstractUserRole.userRoleTypePermissions.put(userRoleType, fixedPermissions);
		}
	}
}
