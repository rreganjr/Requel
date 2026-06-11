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
 * Marks a command instance that should bypass {@code AuthorizingCommandHandler}'s permission
 * check for this invocation, because it is an intrinsic sub-step of an already-authorized parent
 * command (e.g. the container/relation detach commands a delete runs as part of deleting an
 * entity). Direct invocations of the same command type remain fully authorized.
 *
 * <p>TODO(#75): TEMPORARY. This is a stop-gap so a {@code Delete}-only stakeholder is not
 * re-checked for {@code Edit} on every container during a delete cascade. It should be removed
 * once the stakeholder permission-coherence model is implemented:
 * https://github.com/rreganjr/Requel/issues/75
 */
public interface AuthorizationExemptable {

    boolean isAuthorizationExempt();

    void setAuthorizationExempt(boolean authorizationExempt);
}
