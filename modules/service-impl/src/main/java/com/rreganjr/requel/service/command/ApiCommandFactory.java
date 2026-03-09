package com.rreganjr.requel.service.command;

import com.rreganjr.command.Command;
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
     * The command is ready to be passed to the CommandHandler chain.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Command newCommand(String commandType, Object input) {
        CommandRegistration reg = registry.lookup(commandType);
        Command cmd = (Command) reg.factoryMethod().get();
        if (reg.inputApplicator() != null && input != null) {
            reg.inputApplicator().accept(cmd, input);
        }
        return cmd;
    }
}
