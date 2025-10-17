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

import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRole;
import com.rreganjr.requel.user.exception.NoSuchOrganizationException;
import com.rreganjr.requel.user.exception.NoSuchUserException;

/**
 * Repository abstraction for interacting with identity users and related metadata.
 */
public interface UserRepository extends com.rreganjr.repository.Repository {

    Organization findOrganizationByName(String name) throws NoSuchOrganizationException;

    Set<Organization> findOrganizations();

    Set<String> getOrganizationNames();

    User findUserByUsername(String username) throws NoSuchUserException;

    UserSet findUsers();

    UserSet findUsersForRole(Class<? extends UserRole> roleType);

    Set<Class<? extends UserRole>> findUserRoleTypes();

    UserRolePermission findUserRolePermission(Class<? extends UserRole> userRoleType, String name);

    Set<UserRolePermission> findUserRolePermissions(Class<? extends UserRole> userRoleType);

}
