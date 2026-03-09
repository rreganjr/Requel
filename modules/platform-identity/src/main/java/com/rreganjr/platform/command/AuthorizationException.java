package com.rreganjr.platform.command;

/**
 * Thrown by {@link AuthorizableCommand} authorization checks when the current
 * user does not have the required permission to execute a command or access a resource.
 * Mapped to HTTP 403 Forbidden by the REST exception handler.
 */
public class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
