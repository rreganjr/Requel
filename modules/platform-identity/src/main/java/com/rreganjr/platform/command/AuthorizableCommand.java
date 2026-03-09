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
