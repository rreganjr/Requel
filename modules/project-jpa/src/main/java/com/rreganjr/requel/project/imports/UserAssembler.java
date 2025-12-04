/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
package com.rreganjr.requel.project.imports;

import com.rreganjr.requel.imports.AggregateAssembler;
import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.ImportUnitOfWork;
import com.rreganjr.requel.imports.identity.UserImportDraft;
import com.rreganjr.requel.imports.identity.UserRoleImportDraft;
import com.rreganjr.requel.user.Organization;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.OrganizationImpl;
import com.rreganjr.requel.user.impl.UserImpl;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.user.impl.JpaUserRolePermission;
import com.rreganjr.requel.user.UserRole;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Assembles users from drafts; creates new users if not found.
 */
public class UserAssembler implements AggregateAssembler<UserImportDraft, com.rreganjr.requel.user.User> {

    private final UserRepository userRepository;

    public UserAssembler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Class<UserImportDraft> draftType() {
        return UserImportDraft.class;
    }

    @Override
    public Class<com.rreganjr.requel.user.User> aggregateType() {
        return com.rreganjr.requel.user.User.class;
    }

    @Override
    public com.rreganjr.requel.user.User assemble(UserImportDraft draft, ImportUnitOfWork unitOfWork) throws ImportException {
        if (draft == null) {
            throw new ImportException("user draft is required");
        }
        // Resolve by externalId or username if already present.
        if (StringUtils.hasText(draft.getExternalId())) {
            Optional<com.rreganjr.requel.user.User> cached = unitOfWork.resolve(com.rreganjr.requel.user.User.class, draft.getExternalId());
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        Optional<com.rreganjr.requel.user.User> byUsername = unitOfWork.resolve(com.rreganjr.requel.user.User.class, draft.getUsername());
        if (byUsername.isPresent()) {
            return byUsername.get();
        }
        try {
            com.rreganjr.requel.user.User existing = userRepository.findUserByUsername(draft.getUsername());
            unitOfWork.register(com.rreganjr.requel.user.User.class, draft.getExternalId(), existing);
            unitOfWork.register(com.rreganjr.requel.user.User.class, draft.getUsername(), existing);
            return existing;
        } catch (Exception ignored) {
            // create a new user below
        }

        Organization org = resolveOrganization(draft.getOrganizationName(), unitOfWork);
        UserImpl user = new UserImpl(draft.getUsername(), "imported", draft.getName(), draft.getEmail(), null, org, draft.isEditable());
        hydratePassword(user, draft);
        hydrateRoles(user, draft.getRoles());
        ensureDefaultRole(user);

        // Persist the new user immediately so downstream associations (e.g., UserStakeholder)
        // don’t reference a transient instance.
        user = (UserImpl) userRepository.persist(user);

        unitOfWork.register(com.rreganjr.requel.user.User.class, draft.getExternalId(), user);
        unitOfWork.register(com.rreganjr.requel.user.User.class, draft.getUsername(), user);
        unitOfWork.register(com.rreganjr.requel.user.impl.UserImpl.class, draft.getExternalId(), user);
        return user;
    }

    private Organization resolveOrganization(String name, ImportUnitOfWork unitOfWork) {
        if (StringUtils.hasText(name)) {
            // First consult the import cache
            if (unitOfWork != null) {
                Optional<Organization> cached = unitOfWork.resolve(Organization.class, name);
                if (cached.isPresent()) {
                    return cached.get();
                }
            }
            try {
                return userRepository.findOrganizationByName(name);
            } catch (Exception ignored) {
                OrganizationImpl org = new OrganizationImpl(name);
                org = (OrganizationImpl) userRepository.persist(org);
                if (unitOfWork != null) {
                    unitOfWork.register(Organization.class, name, org);
                }
                return org;
            }
        }
        return null;
    }

    private void hydratePassword(UserImpl user, UserImportDraft draft) {
        if (StringUtils.hasText(draft.getEncryptedPassword())) {
            user.setEncryptedPassword(draft.getEncryptedPassword());
        }
        if (StringUtils.hasText(draft.getPasswordSalt())) {
            user.setPasswordSalt(draft.getPasswordSalt());
        }
        if (StringUtils.hasText(draft.getPasswordAlgorithm())) {
            user.setPasswordEncryptingAlgorithmName(draft.getPasswordAlgorithm());
        }
        if (StringUtils.hasText(draft.getPasswordIterations())) {
            try {
                user.setPasswordEncryptingIterations(Integer.valueOf(draft.getPasswordIterations()));
            } catch (NumberFormatException ignored) {
                // leave default iterations
            }
        }
    }

    private void hydrateRoles(UserImpl user, java.util.List<UserRoleImportDraft> roles) {
        if (roles == null) {
            return;
        }
        for (UserRoleImportDraft roleDraft : roles) {
            String type = roleDraft.getRoleType();
            if ("com.rreganjr.requel.project.ProjectUserRole".equals(type)) {
                if (!user.hasRole(ProjectUserRole.class)) {
                    user.grantRole(ProjectUserRole.class);
                    UserRole role = user.getRoleForType(ProjectUserRole.class);
                    roleDraft.getPermissionNames().forEach(name -> role.grantUserRolePermission(
                            new JpaUserRolePermission(ProjectUserRole.class, name)));
                }
            } else if ("com.rreganjr.requel.user.impl.SystemAdminUserRole".equals(type)) {
                if (!user.hasRole(SystemAdminUserRole.class)) {
                    user.grantRole(SystemAdminUserRole.class);
                }
            }
        }
    }

    /**
     * Ensure we satisfy validation that a user must have at least one role.
     * If nothing was provided in the XML, default to ProjectUserRole.
     */
    private void ensureDefaultRole(UserImpl user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            user.grantRole(ProjectUserRole.class);
        }
    }
}
