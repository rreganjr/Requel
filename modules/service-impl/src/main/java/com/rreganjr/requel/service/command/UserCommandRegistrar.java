package com.rreganjr.requel.service.command;

import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.api.dto.EditUserInput;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.user.command.UserCommandFactory;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registers user domain command types with the CQRS command registry at startup.
 * Note: Login is handled by AuthController (JWT-based), not the command dispatch endpoint.
 */
@Component
public class UserCommandRegistrar {

    private static final Logger log = LoggerFactory.getLogger(UserCommandRegistrar.class);

    private final UserCommandFactory factory;
    private final UserRepository userRepository;
    private final CommandRegistry registry;

    public UserCommandRegistrar(UserCommandFactory factory, UserRepository userRepository,
                                CommandRegistry registry) {
        this.factory = factory;
        this.userRepository = userRepository;
        this.registry = registry;
    }

    @PostConstruct
    void registerCommands() {
        registry.register("Login", factory::newLoginCommand);

        registry.register("EditUser", EditUserInput.class,
                factory::newEditUserCommand,
                (cmd, input) -> {
                    EditUserCommand c = (EditUserCommand) cmd;
                    EditUserInput i = (EditUserInput) input;

                    // If username matches an existing user, set it for update; otherwise leave null for create
                    if (i.username() != null) {
                        try {
                            c.setUser(userRepository.findUserByUsername(i.username()));
                        } catch (NoSuchUserException e) {
                            // New user — leave user null, command will create
                        }
                    }

                    c.setUsername(i.username());
                    if (i.password() != null) {
                        c.setPassword(i.password());
                        c.setRepassword(i.repassword());
                    }
                    if (i.name() != null) c.setName(i.name());
                    if (i.emailAddress() != null) c.setEmailAddress(i.emailAddress());
                    if (i.phoneNumber() != null) c.setPhoneNumber(i.phoneNumber());
                    if (i.organizationName() != null) c.setOrganizationName(i.organizationName());
                    if (i.editable() != null) c.setEditable(i.editable());
                    if (i.userRoleNames() != null) c.setUserRoleNames(i.userRoleNames());
                    if (i.userRolePermissionNames() != null) c.setUserRolePermissionNames(i.userRolePermissionNames());
                });

        log.info("Registered {} user command types", 2);
    }
}
