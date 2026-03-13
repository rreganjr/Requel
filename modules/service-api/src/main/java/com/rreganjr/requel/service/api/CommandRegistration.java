package com.rreganjr.requel.service.api;

import com.rreganjr.command.Command;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Binds a command type string to its input DTO class, factory method, and
 * applicators. Per-domain registrars register these at startup.
 * <p>
 * The input applicator maps a deserialized input DTO onto the command's
 * setter-based API. The file applicator bridges a multipart file onto the
 * command (e.g., MultipartFile → InputStream). The result extractor converts
 * the command's domain entity result into an API DTO for the response.
 * <p>
 * This keeps input/output mapping external to the domain command, so existing
 * commands don't need modification. MultipartFile and DTO types never leak
 * into domain interfaces.
 *
 * @param commandType      the command type string (e.g. "EditGoal", "NewUser")
 * @param inputClass       the input DTO class for JSON deserialization (Void.class if no input)
 * @param factoryMethod    supplies a new, uninitialized command instance from the domain factory
 * @param inputApplicator  maps the deserialized input DTO onto the command; null if no input mapping
 * @param fileApplicator   maps a multipart file onto the command; null if the command doesn't accept files
 * @param resultExtractor  extracts and converts the command result to an API DTO; null if no result
 */
public record CommandRegistration<T>(
        String commandType,
        Class<T> inputClass,
        Supplier<Command> factoryMethod,
        BiConsumer<Command, T> inputApplicator,
        BiConsumer<Command, Object> fileApplicator,
        Function<Command, Object> resultExtractor
) {
}
