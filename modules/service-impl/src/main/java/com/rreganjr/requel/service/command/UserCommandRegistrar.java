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

import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.api.dto.EditUserInput;
import com.rreganjr.requel.service.auth.UserDtoMapper;
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
    private final UserDtoMapper userDtoMapper;

    public UserCommandRegistrar(UserCommandFactory factory, UserRepository userRepository,
                                CommandRegistry registry, UserDtoMapper userDtoMapper) {
        this.factory = factory;
        this.userRepository = userRepository;
        this.registry = registry;
        this.userDtoMapper = userDtoMapper;
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
                },
                null,
                cmd -> userDtoMapper.toDto(((EditUserCommand) cmd).getUser()));

        log.info("Registered {} user command types", 2);
    }
}
