package com.rreganjr.requel.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.platform.command.AuthorizationException;
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

    public CommandController(ApiCommandFactory apiCommandFactory,
                             CommandHandler commandHandler,
                             ObjectMapper objectMapper) {
        this.apiCommandFactory = apiCommandFactory;
        this.commandHandler = commandHandler;
        this.objectMapper = objectMapper;
    }

    /**
     * JSON-only command dispatch.
     */
    @PostMapping(value = "/{commandType}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> dispatchJson(
            @PathVariable String commandType,
            @RequestBody(required = false) Map<String, Object> rawInput) {
        return dispatch(commandType, rawInput, null);
    }

    /**
     * Multipart command dispatch — JSON input as a part, file as a part.
     * Used for commands that accept file uploads (e.g., ImportProject).
     */
    @PostMapping(value = "/{commandType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> dispatchMultipart(
            @PathVariable String commandType,
            @RequestPart(value = "input", required = false) Map<String, Object> rawInput,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return dispatch(commandType, rawInput, file);
    }

    private ResponseEntity<?> dispatch(String commandType, Map<String, Object> rawInput,
                                       MultipartFile file) {
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

            // Extract result DTO via the registration's resultExtractor (null if not registered)
            Object result = apiCommandFactory.extractResult(commandType, command);
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
