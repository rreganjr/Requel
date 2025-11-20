/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
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
package com.rreganjr.platform.identity;

import com.rreganjr.requel.user.exception.NoSuchRoleForUserException;

import java.util.Comparator;

/**
 * Minimal identity projection exposed to downstream modules.
 */
public interface User extends Comparable<User> {

    /**
     * @return stable database identifier, or {@code null} for transient records.
     */
    Long getId();

    /**
     * @return unique login name.
     */
    String getUsername();

    /**
     * Return the role object for the specified type. A user can only have one
     * role per type.
     *
     * @param <T> -
     *            the class of user role being retrieved
     * @param roleType -
     *            The type (class) of the role being requested.
     * @return the role object for the specified type
     * @throws NoSuchRoleForUserException -
     *             if the user doesn't have a role for the supplied type
     */
    <T extends Role> T getRoleForType(Class<T> roleType)
            throws NoSuchRoleForUserException;

    /**
     * Return true if the user is assigned to the supplied role type.
     *
     * @param roleType -
     *            The UserRoleType of the role being tested.
     * @return - true if the user is assigned to the supplied role type.
     */
    boolean hasRole(Class<? extends Role> roleType);


    /**
     * Optional display helper; defaults to username when not overridden.
     */
    default String getDisplayName() {
        return getUsername();
    }


    /**
     * a Comparator for comparing two users, ordered by username
     */
    Comparator<User> UserComparator = new Comparator<>() {
        public int compare(User o1, User o2) {
            // this catches the case of when a user's username has changed
            // TODO: if the new username sorts before the original then the
            // username comparator may terminate the sorting before the actual
            // user is found.
            if (o1.equals(o2)) {
                return 0;
            }
            return UsernameComparator.compare(o1.getUsername(), o2.getUsername());
        }
    };

    /**
     * Compare two username strings.
     */
    Comparator<String> UsernameComparator = Comparator.comparing(String::toLowerCase);

}
