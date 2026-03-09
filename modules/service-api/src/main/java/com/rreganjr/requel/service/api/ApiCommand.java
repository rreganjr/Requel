package com.rreganjr.requel.service.api;

/**
 * Implemented by domain Command classes to accept input from the CQRS API layer.
 * The input type T is a simple DTO (Java record) matching the JSON shape the
 * Angular frontend sends.
 *
 * @param <T> the input DTO type for this command
 */
public interface ApiCommand<T> {

    /**
     * Apply the deserialized input DTO fields onto this command instance.
     * Called after the command is created by its domain factory and before
     * it is passed to the CommandHandler chain.
     */
    void applyInput(T input);
}
