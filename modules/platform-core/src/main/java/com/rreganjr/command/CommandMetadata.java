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
package com.rreganjr.command;

/**
 * Carries supplemental data about a command that is set by the API dispatch
 * layer (factory/controller) and read by cross-cutting handlers (auditing,
 * logging). Stored on the command via {@link CommandMetadataAware}.
 * <p>
 * Fields:
 * <ul>
 *   <li>{@code commandType} — the URL dispatch string (e.g. "EditProject")</li>
 *   <li>{@code input} — the typed input DTO that was applied to the command</li>
 * </ul>
 */
public class CommandMetadata {

    private String commandType;
    private Object input;

    public CommandMetadata() {}

    public CommandMetadata(String commandType, Object input) {
        this.commandType = commandType;
        this.input = input;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public Object getInput() {
        return input;
    }

    public void setInput(Object input) {
        this.input = input;
    }
}
