/*
 * $Id: EditUserCommand.java,v 1.3 2009/02/13 12:08:07 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.user.command;

import java.util.Map;
import java.util.Set;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.requel.user.User;

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
