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

/**
 * Sentences shared by several {@link CommandDescription}s (issue #296).
 *
 * <p>An MCP client reads each tool description on its own, so a rule that applies to many
 * commands is repeated in each of them rather than stated once somewhere the caller may never
 * look. Keeping the sentence here keeps the copies identical. Annotation values accept
 * compile-time constant concatenation, so a description appends these with {@code +}.
 *
 * <p>Each constant starts with a space, so it can follow a sentence directly.
 *
 * @author ron
 */
public final class CommandDescriptions {

    /**
     * The #316 partial-update contract, for an Edit command whose optional fields follow it. Say
     * separately which fields are required on every call (a {@code @NotBlank} name is).
     */
    public static final String PARTIAL_UPDATE = " When editing, an optional field you leave null or"
            + " omit keeps its current value, and an empty string clears it.";

    /**
     * The optimistic-lock rule for a delete that checks {@code version} (issue #296 wired it into
     * the seven entity deletes, which had accepted it and dropped it).
     */
    public static final String VERSION_CHECKED_ON_DELETE = " Pass the version you last read to"
            + " have the delete refused if someone else saved first; a null version skips the"
            + " check.";

    /**
     * The optimistic-lock rule for a command that checks {@code version}. The check is skipped
     * when {@code version} is null.
     */
    public static final String VERSION_CHECKED = " When editing, pass the version you last read to"
            + " have the change refused if someone else saved first; a null version skips the"
            + " check.";

    private CommandDescriptions() {
    }
}
