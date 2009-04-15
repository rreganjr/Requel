/*
 * $Id: JAXBUserRolePatcher.java,v 1.1 2009/01/07 09:50:37 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.utils.jaxb;

import org.xml.sax.SAXException;

import com.sun.istack.SAXException2;
import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;

import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.UserRole;
import edu.harvard.fas.rregan.requel.user.UserRolePermission;

/**
 * This is used to swap the UserRolePermission object on UserRole objects
 * assigned by the JAXB Unmarshaller with existing permissions from the
 * database. It also patches the user
 * 
 * @author ron
 */
public class JAXBUserRolePatcher implements Patcher {

	private final UserRole userRole;
	private final UserRepository userRepository;

	/**
	 * @param userRepository
	 * @param userRole
	 */
	public JAXBUserRolePatcher(UserRepository userRepository, UserRole userRole) {
		this.userRepository = userRepository;
		this.userRole = userRole;
	}

	@Override
	public void run() throws SAXException {
		try {
			for (UserRolePermission permission : userRepository.findUserRolePermissions(userRole
					.getClass())) {
				if (userRole.hasUserRolePermission(permission)) {
					userRole.revokeUserRolePermission(permission);
					userRole.grantUserRolePermission(permission);
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SAXException2(e);
		}
	}
}
