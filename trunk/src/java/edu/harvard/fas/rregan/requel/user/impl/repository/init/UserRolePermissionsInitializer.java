/*
 * $Id: UserRolePermissionsInitializer.java,v 1.7 2009/01/26 10:19:03 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user.impl.repository.init;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.harvard.fas.rregan.AbstractSystemInitializer;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.requel.user.AbstractUserRole;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.UserRole;
import edu.harvard.fas.rregan.requel.user.UserRolePermission;

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
