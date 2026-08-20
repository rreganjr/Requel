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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.service.api.CommandResult;
import com.rreganjr.requel.service.api.dto.ErrorResponse;
import com.rreganjr.repository.jpa.BeanValidationException;
import com.rreganjr.validator.EntityValidationException;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Single dispatch endpoint for all command mutations.
 * POST /api/commands/{commandType}
 */
@RestController
@RequestMapping("/api/commands")
public class CommandController {

    private static final Logger log = LoggerFactory.getLogger(CommandController.class);

    private final ApiCommandFactory apiCommandFactory;
    private final CommandHandler commandHandler;
    private final ObjectMapper objectMapper;
    private final CommandEventPublisher eventPublisher;

    public CommandController(ApiCommandFactory apiCommandFactory,
                             CommandHandler commandHandler,
                             ObjectMapper objectMapper,
                             CommandEventPublisher eventPublisher) {
        this.apiCommandFactory = apiCommandFactory;
        this.commandHandler = commandHandler;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * JSON-only command dispatch.
     */
    @PostMapping(value = "/{commandType}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> dispatchJson(
            @PathVariable String commandType,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestBody(required = false) Map<String, Object> rawInput) {
        return dispatch(commandType, rawInput, null, sessionId);
    }

    /**
     * Multipart command dispatch — JSON input as a part, file as a part.
     * Used for commands that accept file uploads (e.g., ImportProject).
     */
    @PostMapping(value = "/{commandType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> dispatchMultipart(
            @PathVariable String commandType,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestPart(value = "input", required = false) Map<String, Object> rawInput,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return dispatch(commandType, rawInput, file, sessionId);
    }

    private ResponseEntity<?> dispatch(String commandType, Map<String, Object> rawInput,
                                       MultipartFile file, String sessionId) {
        try {
            // Deserialize raw JSON to the command's input DTO type (null for commands with no input mapping yet)
            Class<?> inputType = apiCommandFactory.getInputType(commandType);
            Object input = (inputType != Void.class && rawInput != null)
                    ? objectMapper.convertValue(rawInput, inputType)
                    : null;

            // Create and configure the command — editedBy is set by CurrentUserCommandHandler in the chain
            Command command = apiCommandFactory.newCommand(commandType, input, file);

            // Execute through the handler chain
            commandHandler.execute(command);

            // Extract result DTOs via the registration's extractors (null if not registered)
            Object result = apiCommandFactory.extractResult(commandType, command);
            Object secondaryResult = apiCommandFactory.extractSecondaryResult(commandType, command);

            // Publish SSE events: the Project:0 broadcast (all sidebar sessions) plus targeted
            // events for the primary and secondary entities, excluding the originating session from
            // the targeted events so it does not reload the form it just edited. A missing header
            // (sessionId == null) excludes nobody.
            eventPublisher.publish(command, result, secondaryResult, sessionId);

            return ResponseEntity.ok(CommandResult.success(result, commandType));

        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse.of("FORBIDDEN", e.getMessage()));
        } catch (OptimisticLockException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "success", false,
                            "error", "Conflict",
                            "message", "Entity was modified by another user. Please reload and try again."));
        } catch (EntityValidationException e) {
            var violations = new java.util.ArrayList<CommandResult.FieldViolation>();
            if (e instanceof BeanValidationException bve) {
                String[] propNames = bve.getEntityPropertyNames();
                String[] fieldMessages = bve.getFieldMessages();
                if (propNames != null) {
                    for (int i = 0; i < propNames.length; i++) {
                        violations.add(new CommandResult.FieldViolation(propNames[i], fieldMessages[i]));
                    }
                }
            } else {
                String[] propNames = e.getEntityPropertyNames();
                if (propNames != null) {
                    for (String prop : propNames) {
                        violations.add(new CommandResult.FieldViolation(prop, e.getMessage()));
                    }
                }
            }
            if (violations.isEmpty()) {
                violations.add(new CommandResult.FieldViolation(null, e.getMessage()));
            }
            return ResponseEntity.unprocessableEntity()
                    .body(CommandResult.validationFailure("Validation failed", violations));
        } catch (EntityException e) {
            log.warn("Command business rule violation: {} - {}", commandType, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.of("CONFLICT", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("Command execution failed: {} - {}", commandType, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred. Please try again or contact support."));
        }
    }
}
