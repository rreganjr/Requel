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
package com.rreganjr.requel.service.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The caller-facing description of a gateway command, declared on its input DTO.
 *
 * <p>This is the only per-command prose that reaches a caller. {@code GatewayCommandCatalogImpl}
 * reads it off the input type when building a {@code CommandDescriptor}, and from there it becomes
 * the MCP tool description and the generated CLI help. Javadoc does not: it is not retained at
 * runtime, so a note written only in a doc comment is visible to whoever opens the file and to
 * nobody else.
 *
 * <p>Write it for someone calling the command without the source in front of them. Say what the
 * command does to state, what the id-versus-name fields select, and above all anything the caller
 * would otherwise discover by surprise — a value transformed on write, or an edit refused rather
 * than resolved. A field list is already generated; do not restate it.
 *
 * <p>Every command on the gateway allowlist must carry one (issue #296; {@code
 * McpToolCatalogLockstepIT} fails the build otherwise). Absent on an input type, the descriptor's
 * description stays {@code null} and the MCP layer falls back to the humanized command name plus
 * the input's field names.
 *
 * <p>Do not state the permission a command needs: the catalog derives that from the command's
 * {@code getAuthorizationRequirement()} and publishes it as the descriptor's authorization hint.
 * Shared sentences, such as the partial-update rule, live in {@link CommandDescriptions}.
 *
 * @author ron
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandDescription {

    /** The description, written for a caller. */
    String value();

    /**
     * Overrides the authorization hint the catalog derives from the command, for a command whose
     * requirement depends on its input (a project tag versus a global one, creating a project
     * versus editing one). The derived hint comes from a command created with no input, so for
     * those commands it would be wrong. Empty, the default, means derive it.
     */
    String authorization() default "";
}
