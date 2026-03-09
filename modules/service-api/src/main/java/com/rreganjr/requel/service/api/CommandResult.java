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
