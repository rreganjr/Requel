/*
 * $Id: UserRepository.java,v 1.16 2008/12/13 00:41:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.user;

import java.util.Set;

import edu.harvard.fas.rregan.repository.Repository;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchOrganizationException;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;

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
	public UserSet findUsers();

	/**
	 * @param roleType
	 * @return a set of users that have the supplied role type.
	 */
	public UserSet findUsersForRole(Class<? extends UserRole> roleType);

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
