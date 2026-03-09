package com.rreganjr.requel.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.service.api.CommandResult;
import com.rreganjr.requel.service.api.dto.ErrorResponse;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.validator.EntityValidationException;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CommandController(ApiCommandFactory apiCommandFactory,
                             CommandHandler commandHandler,
                             CurrentUserResolver currentUserResolver,
                             ObjectMapper objectMapper) {
        this.apiCommandFactory = apiCommandFactory;
        this.commandHandler = commandHandler;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{commandType}")
    public ResponseEntity<?> dispatch(
            @PathVariable String commandType,
            @RequestBody(required = false) Map<String, Object> rawInput) {
        try {
            // Deserialize raw JSON to the command's input DTO type (null for commands with no input mapping yet)
            Class<?> inputType = apiCommandFactory.getInputType(commandType);
            Object input = (inputType != Void.class && rawInput != null)
                    ? objectMapper.convertValue(rawInput, inputType)
                    : null;

            // Create and configure the command
            Command command = apiCommandFactory.newCommand(commandType, input);
            if (command instanceof EditCommand editCmd) {
                editCmd.setEditedBy(currentUserResolver.resolve());
            }

            // Execute through the handler chain
            commandHandler.execute(command);

            // TODO: extract result entity/DTO from command when ApiCommand supports it
            return ResponseEntity.ok(CommandResult.success(null, commandType));

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
            String[] propNames = e.getEntityPropertyNames();
            if (propNames != null) {
                for (String prop : propNames) {
                    violations.add(new CommandResult.FieldViolation(prop, e.getMessage()));
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
                    .body(ErrorResponse.of("INTERNAL_ERROR", "Command execution failed"));
        }
    }
}
