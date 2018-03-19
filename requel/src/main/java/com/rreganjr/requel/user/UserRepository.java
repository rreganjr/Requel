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

import java.util.Set;

import com.rreganjr.repository.Repository;
import com.rreganjr.requel.user.exception.NoSuchOrganizationException;
import com.rreganjr.requel.user.exception.NoSuchUserException;

/**
 * @author ron
 */
public interface UserRepository extends Repository {

	/**
	 * Get a specific organization by name.
	 * 
	 * @param name -
	 *            the name of the organization
	 * @return the organzation with the specified name
	 * @throws NoSuchOrganizationException
	 */
	public Organization findOrganizationByName(String name) throws NoSuchOrganizationException;

	/**
	 * @return all the organizations in the system.
	 */
	public Set<Organization> findOrganizations();

	/**
	 * @return all the names of the organizations in the system.
	 */
	public Set<String> getOrganizationNames();

	/**
	 * Get a specific user by username from the repository.
	 * 
	 * @param username -
	 *            the username of the user to retrieve from the repository.
	 * @return The user for the username
	 * @throws NoSuchUserException -
	 *             if the supplied username doesn't correspond to a user in the
	 *             repository.
	 */
	public User findUserByUsername(String username) throws NoSuchUserException;

	/**
	 * @return all the users of the system in the repository.
	 */
	public Set<User> findUsers();

	/**
	 * @param roleType
	 * @return a set of users that have the supplied role type.
	 */
	public Set<User> findUsersForRole(Class<? extends UserRole> roleType);

	/**
	 * @return the available types of user roles
	 */
	public Set<Class<? extends UserRole>> findUserRoleTypes();

	/**
	 * @param userRoleType
	 * @param name
	 * @return
	 */
	public UserRolePermission findUserRolePermission(Class<? extends UserRole> userRoleType,
			String name);

	/**
	 * @param userRoleType
	 * @return
	 */
	public Set<UserRolePermission> findUserRolePermissions(Class<? extends UserRole> userRoleType);
}
