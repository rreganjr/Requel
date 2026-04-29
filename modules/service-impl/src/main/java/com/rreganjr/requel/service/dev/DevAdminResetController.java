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
package com.rreganjr.requel.service.dev;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.user.command.UserCommandFactory;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only endpoint that resets the admin user's password to the configured default.
 * Only registered when {@code requel.dev.reset-admin.enabled=true}.
 *
 * <p>Used by the E2E global-setup to guarantee admin/admin credentials work before
 * each test run, regardless of what happened to the password in a prior run.</p>
 *
 * <p>The endpoint is unauthenticated (see ApiSecurityConfig {@code /api/dev/**} permitAll).
 * It is safe in production because the bean is absent unless the property is explicitly set.</p>
 */
@ConditionalOnProperty(name = "requel.dev.reset-admin.enabled", havingValue = "true")
@RestController
@RequestMapping("/api/dev")
public class DevAdminResetController {

    private final UserCommandFactory userCommandFactory;
    private final UserRepository userRepository;
    private final CommandHandler commandHandler;

    @Value("${requel.admin.password:admin}")
    private String adminPassword;

    @Autowired
    public DevAdminResetController(UserCommandFactory userCommandFactory,
                                   UserRepository userRepository,
                                   CommandHandler commandHandler) {
        this.userCommandFactory = userCommandFactory;
        this.userRepository = userRepository;
        this.commandHandler = commandHandler;
    }

    /**
     * Resets the admin user's password to {@code requel.admin.password} (default: "admin").
     * All other fields (name, email, phone, org, roles) are preserved.
     *
     * <p>Sets editedBy to admin itself (admin editing own account) so the authorization
     * chain allows the update without requiring a live security context.</p>
     *
     * @return 204 No Content on success, 404 if admin does not exist, 500 on error.
     */
    @PostMapping("/reset-admin")
    public ResponseEntity<Void> resetAdmin() {
        try {
            User admin = userRepository.findUserByUsername("admin");
            String orgName = admin.getOrganization() != null
                    ? admin.getOrganization().getName()
                    : null;

            EditUserCommand cmd = userCommandFactory.newEditUserCommand();
            cmd.setUser(admin);
            // admin editing own account: auth requirement returns null → no role check
            cmd.setEditedBy(admin);
            cmd.setUsername(admin.getUsername());
            cmd.setName(admin.getName());
            cmd.setEmailAddress(admin.getEmailAddress());
            cmd.setPhoneNumber(admin.getPhoneNumber());
            cmd.setOrganizationName(orgName);
            cmd.setPassword(adminPassword);
            cmd.setRepassword(adminPassword);
            // userRoleNames not set → userRolesProvided=false → existing roles preserved
            commandHandler.execute(cmd);
            return ResponseEntity.noContent().build();
        } catch (NoSuchUserException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
