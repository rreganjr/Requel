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
package com.rreganjr.requel.utils.jaxb;

import org.xml.sax.SAXException;

import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;

import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.UserRole;
import com.rreganjr.requel.user.UserRolePermission;

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
			throw new SAXException(e);
		}
	}
}
