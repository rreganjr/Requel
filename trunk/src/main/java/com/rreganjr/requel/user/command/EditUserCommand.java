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
package com.rreganjr.requel.user.command;

import java.util.Map;
import java.util.Set;

import com.rreganjr.command.Command;
import com.rreganjr.requel.user.User;

/**
 * @author ron
 */
public interface EditUserCommand extends Command {

	/**
	 * @return the new or updated user
	 */
	public User getUser();

	/**
	 * set the user to edit, if not set a new user will be created.
	 * 
	 * @param user -
	 *            the user to edit.
	 */
	public void setUser(User user);

	/**
	 * Set the username of the user.
	 * 
	 * @param username
	 */
	public void setUsername(String username);

	/**
	 * Set the password of the user
	 * 
	 * @param password
	 */
	public void setPassword(String password);

	/**
	 * Set the repeated password
	 * 
	 * @param repassword
	 */
	public void setRepassword(String repassword);

	/**
	 * Set the name of the user.
	 * 
	 * @param name
	 */
	public void setName(String name);

	/**
	 * Set the email address of the user.
	 * 
	 * @param emailAddress
	 */
	public void setEmailAddress(String emailAddress);

	/**
	 * Set the phoneNumber of the user.
	 * 
	 * @param phoneNumber
	 */
	public void setPhoneNumber(String phoneNumber);

	/**
	 * Set the default organization name of the user. if the organization
	 * doesn't exist it will be created.
	 * 
	 * @param organizationName
	 */
	public void setOrganizationName(String organizationName);

	/**
	 * Set true if this user account can be edited by the user.
	 * 
	 * @param editable
	 */
	public void setEditable(Boolean editable);

	/**
	 * Set the names of the user roles this user has.
	 * 
	 * @param userRoleNames
	 */
	public void setUserRoleNames(Set<String> userRoleNames);

	/**
	 * Add a role by name to grant to the user.
	 * 
	 * @param userRoleName
	 */
	public void addUserRoleName(String userRoleName);

	/**
	 * Set the names of the permissions for each of the roles that the user has.
	 * 
	 * @param userRolePermissionNames
	 */
	public void setUserRolePermissionNames(Map<String, Set<String>> userRolePermissionNames);

	/**
	 * Add a permission for a role name to grant to the user.
	 * 
	 * @param userRoleName
	 * @param userRolePermissionName
	 */
	public void addUserRolePermissionName(String userRoleName, String userRolePermissionName);

	/**
	 * @param editedBy -
	 *            the user invoking the command
	 */
	public void setEditedBy(User editedBy);
}
