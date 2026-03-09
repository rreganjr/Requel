package com.rreganjr.requel.project;

/**
 * A command that operates within a specific project context.
 * Used by AuthorizingCommandHandler to resolve the stakeholder
 * and check project-level permissions.
 * <p>
 * Most project commands already have access to the project — they receive
 * it as a setter or resolve it during setup. This interface just exposes
 * it for the handler to read.
 */
public interface ProjectScopedCommand {

    /**
     * @return the project this command operates within
     */
    Project getProject();
}
