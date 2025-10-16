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
package com.rreganjr.requel.user;

import java.util.Comparator;
import java.util.Set;

/**
 * @author ron
 */
public interface UserRole {

	/**
	 * @return the name of the role.
	 */
	public String getRoleName();

	/**
	 * @return the permissions available for this role
	 */
	public Set<UserRolePermission> getAvailableUserRolePermissions();

	/**
	 * Grant the specified permission to the user.
	 * 
	 * @param permission
	 */
	public void grantUserRolePermission(UserRolePermission permission);

	/**
	 * Revoke the specified permission from the user.
	 * 
	 * @param permission
	 */
	public void revokeUserRolePermission(UserRolePermission permission);

	/**
	 * @param permission
	 * @return true if the user has the specified permission with this role.
	 */
	public boolean hasUserRolePermission(UserRolePermission permission);

	/**
	 * Compare user roles by name.
	 * 
	 * @author ron
	 */
	public static class UserRoleComparator implements Comparator<UserRole> {
		public static final UserRoleComparator INSTANCE = new UserRoleComparator();

		public int compare(UserRole o1, UserRole o2) {
			return o1.getRoleName().compareTo(o2.getRoleName());
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof UserRoleComparator);
		}
	}
}
