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

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandMetadata;
import com.rreganjr.command.CommandMetadataAware;
import com.rreganjr.requel.service.api.CommandRegistration;
import com.rreganjr.requel.service.api.CommandRegistry;
import org.springframework.stereotype.Service;

/**
 * Composite CommandFactory facade for the CQRS API layer.
 * Delegates to per-domain factories via the {@link CommandRegistry}.
 * Provides two entry points:
 * <ul>
 *   <li>{@link #getInputType} — returns the input DTO class for JSON deserialization</li>
 *   <li>{@link #newCommand} — creates a command instance and applies the input via the registrar's applicator</li>
 * </ul>
 */
@Service
public class ApiCommandFactory {

    private final CommandRegistry registry;

    public ApiCommandFactory(CommandRegistry registry) {
        this.registry = registry;
    }

    /**
     * Look up the input DTO class for a command type, used by the controller
     * to deserialize the JSON request body before command creation.
     */
    public Class<?> getInputType(String commandType) {
        return registry.lookup(commandType).inputClass();
    }

    /**
     * Create a new command instance and apply input via the registration's applicator.
     * Stamps {@link CommandMetadata} with the command type and input DTO so that
     * downstream handlers (e.g. auditing) can access dispatch context.
     * The command is ready to be passed to the CommandHandler chain.
     */
    public Command newCommand(String commandType, Object input) {
        return newCommand(commandType, input, null);
    }

    /**
     * Create a new command instance with optional file support.
     * For multipart commands, the file is bridged onto the command via the
     * registration's fileApplicator — MultipartFile never leaks into domain interfaces.
     *
     * <p>The {@link CommandMetadata} stamped here is also how {@link ValidatingCommandHandler}
     * recovers the input DTO in order to bean-validate it (issue #171), which is why a command that
     * has an input but cannot carry metadata is rejected outright rather than allowed through
     * unvalidated.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Command newCommand(String commandType, Object input, Object file) {
        CommandRegistration reg = registry.lookup(commandType);
        Command cmd;
        if (reg.commandBuilder() != null) {
            // Polymorphic path: command type depends on input (e.g. ResolveIssue)
            cmd = (Command) reg.commandBuilder().apply(input);
        } else {
            cmd = (Command) reg.factoryMethod().get();
            if (reg.inputApplicator() != null && input != null) {
                reg.inputApplicator().accept(cmd, input);
            }
        }
        if (reg.fileApplicator() != null && file != null) {
            reg.fileApplicator().accept(cmd, file);
        }
        if (cmd instanceof CommandMetadataAware metadataAware) {
            metadataAware.setCommandMetadata(new CommandMetadata(commandType, input));
        } else if (input != null) {
            // Every command reachable from the API extends AbstractCommand and so is
            // CommandMetadataAware. If that ever stops being true, the command would reach the
            // handler chain with no way to recover its input DTO and ValidatingCommandHandler would
            // skip it — silently unvalidated. Fail loudly instead: this is a wiring bug, not input.
            throw new IllegalStateException("Command type '" + commandType + "' produced "
                    + cmd.getClass().getName() + ", which is not CommandMetadataAware, so its input "
                    + "cannot be validated in the handler chain. Make it extend AbstractCommand.");
        }
        return cmd;
    }

    /**
     * @return true if the command type accepts file uploads
     */
    public boolean acceptsFile(String commandType) {
        return registry.lookup(commandType).fileApplicator() != null;
    }

    /**
     * Extract the result DTO from a command after execution.
     * Uses the registration's resultExtractor to convert the domain entity
     * to an API DTO. Returns null if no extractor is registered.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object extractResult(String commandType, Command command) {
        CommandRegistration reg = registry.lookup(commandType);
        if (reg.resultExtractor() != null) {
            return reg.resultExtractor().apply(command);
        }
        return null;
    }
}
