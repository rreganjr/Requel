package com.rreganjr.requel.service.command;

import com.rreganjr.requel.user.command.UserCommandFactory;
import com.rreganjr.requel.service.api.CommandRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registers user domain command types with the CQRS command registry at startup.
 * Note: Login is handled by AuthController (JWT-based), not the command dispatch endpoint.
 * It is registered here for completeness but may not be dispatched via /api/commands.
 */
@Component
public class UserCommandRegistrar {

    private static final Logger log = LoggerFactory.getLogger(UserCommandRegistrar.class);

    private final UserCommandFactory factory;
    private final CommandRegistry registry;

    public UserCommandRegistrar(UserCommandFactory factory, CommandRegistry registry) {
        this.factory = factory;
        this.registry = registry;
    }

    @PostConstruct
    void registerCommands() {
        registry.register("Login", factory::newLoginCommand);
        registry.register("EditUser", factory::newEditUserCommand);

        log.info("Registered {} user command types", 2);
    }
}
