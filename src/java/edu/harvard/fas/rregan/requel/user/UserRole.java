/*
 * $Id: UserRole.java,v 1.8 2008/03/26 10:39:53 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user;

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
