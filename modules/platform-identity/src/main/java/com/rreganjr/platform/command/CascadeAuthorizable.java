/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.platform.command;

/**
 * Issue #75: a command a parent runs as a step of the parent's own operation - a delete removing
 * what it leaves behind (the deleted entity's links, the annotations only on it) or what it alone
 * owned (a use case's unshared scenarios, everything in a deleted project). Such a step is
 * authorized by the permission that authorized the parent, not by its own: deleting a goal needs
 * Goal[Delete], not Goal[Edit] for each container it is detached from or Annotation[Delete] for the
 * notes only on it. The stakeholder page says so ("owned deletes").
 *
 * <p>It is checked, not skipped. {@code AuthorizingCommandHandler} accepts the step only while its
 * authorizing command is executing on the same thread, was itself authorized, and was run by the
 * same user. The same command run on its own is checked against its own requirement as usual.
 *
 * <p>The parent is responsible for scope: it builds steps only for its own leftovers or for what it
 * alone owns. Build them with {@link #cascade(Object, Object)}.
 */
public interface CascadeAuthorizable {

    /** The command whose authorization covers this one, or null when it runs on its own. */
    AuthorizableCommand getAuthorizingCommand();

    void setAuthorizingCommand(AuthorizableCommand authorizingCommand);

    /**
     * Mark {@code step} as a step of {@code parent}'s operation and return it. The authorizing
     * command is the root of the chain: {@code parent}'s own authorizing command when it is itself
     * a step, otherwise {@code parent} when it declares a requirement. A parent with neither (an
     * internal command with no requirement) hands nothing on, and the step is checked against its
     * own requirement.
     */
    static <T> T cascade(Object parent, T step) {
        if (!(step instanceof CascadeAuthorizable cascadeStep)) {
            throw new IllegalArgumentException(
                    step == null ? "null step" : step.getClass().getName() + " can't be a cascade step");
        }
        cascadeStep.setAuthorizingCommand(authorizingRoot(parent));
        return step;
    }

    /** The command whose authorization {@code command}'s steps borrow, or null. */
    static AuthorizableCommand authorizingRoot(Object command) {
        if (command instanceof CascadeAuthorizable step && step.getAuthorizingCommand() != null) {
            return step.getAuthorizingCommand();
        }
        if (command instanceof AuthorizableCommand authorizable
                && authorizable.getAuthorizationRequirement() != null) {
            return authorizable;
        }
        return null;
    }
}
