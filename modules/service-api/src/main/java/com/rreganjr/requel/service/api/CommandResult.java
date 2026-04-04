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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Standard response wrapper for command execution results.
 *
 * @param success    true if the command executed without error
 * @param entity     the created/updated entity DTO (null on failure)
 * @param entityType the entity type name (e.g. "Goal", "User") — null on failure
 * @param error      error message (null on success)
 * @param violations validation violations (null/empty on success)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommandResult(
        boolean success,
        Object entity,
        String entityType,
        String error,
        List<FieldViolation> violations
) {

    public static CommandResult success(Object entity, String entityType) {
        return new CommandResult(true, entity, entityType, null, null);
    }

    public static CommandResult failure(String error) {
        return new CommandResult(false, null, null, error, null);
    }

    public static CommandResult validationFailure(String error, List<FieldViolation> violations) {
        return new CommandResult(false, null, null, error, violations);
    }

    public record FieldViolation(String field, String message) {
    }
}
