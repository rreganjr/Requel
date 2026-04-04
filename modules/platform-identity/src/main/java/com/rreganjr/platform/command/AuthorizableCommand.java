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
 * A command that declares its authorization requirements.
 * The AuthorizingCommandHandler inspects this interface to determine
 * if the current user is permitted to execute the command.
 * <p>
 * Follows the same marker-interface pattern as {@link EditCommand}
 * (which adds setEditedBy) and AnalyzableEditCommand (which adds invokeAnalysis).
 */
public interface AuthorizableCommand extends EditCommand {

    /**
     * The authorization requirement for this command.
     * Returns null if no authorization check is needed (open commands).
     */
    AuthorizationRequirement getAuthorizationRequirement();
}
