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
package com.rreganjr.requel.service.command;

/**
 * A command's input names something that does not exist: an unknown id or name, refused while the
 * command is built from its input. Issue #345: reported as not found (HTTP 404, gateway
 * {@code NOT_FOUND}) rather than as invalid input. It extends {@link IllegalArgumentException}, the
 * type these refusals used before, so code that catches that keeps working.
 */
public class NotFoundInputException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public NotFoundInputException(String message) {
        super(message);
    }
}
