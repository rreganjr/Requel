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
import com.rreganjr.requel.project.ProjectUserRole;
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
 * Dev-only endpoint that resets the project user back to its canonical
 * initializer-defined state. Only registered when
 * {@code requel.dev.reset-project.enabled=true}.
 *
 * <p>Companion to {@link DevAdminResetController}. The E2E global-setup
 * calls this before each run so the suite always starts with the
 * "project" user holding exactly {@code ProjectUserRole} — guarding
 * against drift that can accumulate on developer laptops (e.g., a
 * manual edit promoting "project" to SystemAdminUserRole during
 * debugging, which would invisibly break the {@code adminGuard} e2e
 * tests since they rely on "project" being a non-admin).</p>
 *
 * <p>The endpoint is unauthenticated (see ApiSecurityConfig
 * {@code /api/dev/**} permitAll). It is safe in production because the
 * bean is absent unless the property is explicitly set.</p>
 */
@ConditionalOnProperty(name = "requel.dev.reset-project.enabled", havingValue = "true")
@RestController
@RequestMapping("/api/dev")
public class DevProjectResetController {

    private static final String PROJECT_USERNAME = "project";
    private static final String PROJECT_NAME = "Builtin Project User";

    private final UserCommandFactory userCommandFactory;
    private final UserRepository userRepository;
    private final CommandHandler commandHandler;

    @Value("${requel.project.password:project}")
    private String projectPassword;

    @Autowired
    public DevProjectResetController(UserCommandFactory userCommandFactory,
                                     UserRepository userRepository,
                                     CommandHandler commandHandler) {
        this.userCommandFactory = userCommandFactory;
        this.userRepository = userRepository;
        this.commandHandler = commandHandler;
    }

    /**
     * Resets the project user to canonical state:
     * <ul>
     *   <li>name = "Builtin Project User"</li>
     *   <li>password = {@code requel.project.password} (default "project")</li>
     *   <li>roles = exactly {@code ProjectUserRole} (any drift is removed)</li>
     *   <li>permission = {@code createProjects} on ProjectUserRole</li>
     * </ul>
     *
     * <p>Email, phone, and organization fields are preserved as-is to
     * avoid stamping over any deliberate test fixture setup.</p>
     *
     * <p>Sets editedBy to admin (which has authority over any user) so the
     * authorization chain allows the update without a live security
     * context.</p>
     *
     * @return 204 No Content on success, 404 if either "project" or "admin"
     *         does not exist (admin is needed for editedBy), 500 on error.
     */
    @PostMapping("/reset-project")
    public ResponseEntity<Void> resetProject() {
        try {
            User project = userRepository.findUserByUsername(PROJECT_USERNAME);
            User admin = userRepository.findUserByUsername("admin");
            String orgName = project.getOrganization() != null
                    ? project.getOrganization().getName()
                    : null;

            EditUserCommand cmd = userCommandFactory.newEditUserCommand();
            cmd.setUser(project);
            // admin editing project: admin has SystemAdminUserRole so the
            // authorization requirement for editing any user is satisfied
            cmd.setEditedBy(admin);
            cmd.setUsername(project.getUsername());
            cmd.setName(PROJECT_NAME);
            cmd.setEmailAddress(project.getEmailAddress());
            cmd.setPhoneNumber(project.getPhoneNumber());
            cmd.setOrganizationName(orgName);
            cmd.setPassword(projectPassword);
            cmd.setRepassword(projectPassword);
            // Force roles back to exactly ProjectUserRole — userRoleNames
            // provided => userRolesProvided=true => existing roles are
            // replaced with this set (drift like SystemAdminUserRole is
            // dropped). See EditUserCommandImpl.
            cmd.addUserRoleName(ProjectUserRole.getRoleName(ProjectUserRole.class));
            cmd.addUserRolePermissionName(
                    ProjectUserRole.getRoleName(ProjectUserRole.class),
                    ProjectUserRole.createProjects.getName());
            commandHandler.execute(cmd);
            return ResponseEntity.noContent().build();
        } catch (NoSuchUserException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
