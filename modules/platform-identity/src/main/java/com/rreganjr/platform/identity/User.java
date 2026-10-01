/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
     * The username of the original built-in assistant account (issues #302, #270). Since #260
     * each assistant writes as its own identity ({@link #ASSISTANT_USERNAME_PREFIX} + its id);
     * this account keeps the annotations written before that. Created by
     * {@code AssistantUserInitializer}.
     */
    String ASSISTANT_USERNAME = "assistant";

    /** Issue #260: an assistant's own identity is this prefix followed by its assistant id. */
    String ASSISTANT_USERNAME_PREFIX = "assistant-";

    /**
     * @return true if {@code user} is an assistant identity - it holds an {@link AssistantRole}
     *         (issue #260) or is the original {@code assistant} account; false for null.
     */
    static boolean isAssistant(User user) {
        return user != null && (user.hasRole(AssistantRole.class)
                || ASSISTANT_USERNAME.equals(user.getUsername()));
    }

    /** Issue #260: the username of the identity the assistant {@code assistantId} writes as. */
    static String assistantUsername(String assistantId) {
        return ASSISTANT_USERNAME_PREFIX + assistantId;
    }

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
