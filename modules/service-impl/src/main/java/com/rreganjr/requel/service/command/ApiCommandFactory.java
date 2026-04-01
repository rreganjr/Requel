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
