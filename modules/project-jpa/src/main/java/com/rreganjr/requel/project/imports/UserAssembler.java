/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
import com.rreganjr.requel.user.Organization;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.OrganizationImpl;
import com.rreganjr.requel.user.impl.UserImpl;
import com.rreganjr.requel.project.ProjectUserRole;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Assembles users from drafts; creates new users if not found.
 * <p>
 * #444: a user the import creates starts with a random password nobody knows, so it can't sign
 * in until an administrator resets it. Only when {@code restoreAccounts} is set (the importer is a
 * system administrator, who could create the account anyway) is the file's password hash applied.
 * Roles and role permissions in the file are never applied: a new user gets the default
 * {@code ProjectUserRole}. An existing user is always used as it is.
 */
public class UserAssembler implements AggregateAssembler<UserImportDraft, com.rreganjr.requel.user.User> {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final boolean restoreAccounts;

    /** An assembler that never restores credentials or global roles from the file. */
    public UserAssembler(UserRepository userRepository) {
        this(userRepository, false);
    }

    /**
     * @param restoreAccounts true to apply the file's password hash and global roles to users the
     *            import creates; pass it only for a system administrator's import.
     */
    public UserAssembler(UserRepository userRepository, boolean restoreAccounts) {
        this.userRepository = userRepository;
        this.restoreAccounts = restoreAccounts;
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
        UserImpl user = new UserImpl(draft.getUsername(), unguessablePassword(), draft.getName(), draft.getEmail(), null, org, draft.isEditable());
        if (restoreAccounts) {
            hydratePassword(user, draft);
        }
        // #444: roles and role permissions in the file are never applied; a new user gets the
        // default ProjectUserRole, and an administrator grants anything more.
        ensureDefaultRole(user);

        // Persist the new user immediately so downstream associations (e.g., UserStakeholder)
        // don’t reference a transient instance.
        user = (UserImpl) userRepository.persist(user);

        unitOfWork.register(com.rreganjr.requel.user.User.class, draft.getExternalId(), user);
        unitOfWork.register(com.rreganjr.requel.user.User.class, draft.getUsername(), user);
        unitOfWork.register(com.rreganjr.requel.user.impl.UserImpl.class, draft.getExternalId(), user);
        return user;
    }

    /** 32 random bytes; never logged or returned, so nobody can sign in with it. */
    private static String unguessablePassword() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
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

    /**
     * Restores the file's hash with the settings it was made with. A setting the file leaves out
     * is cleared rather than kept from the random password, so {@link UserImpl} falls back to its
     * defaults (unsalted MD5): that is how a Requel 1.x export, which carries only the hash, signs
     * in. The first successful sign-in re-hashes with the preferred settings. A file with no hash
     * leaves the random password in place.
     */
    private void hydratePassword(UserImpl user, UserImportDraft draft) {
        if (!StringUtils.hasText(draft.getEncryptedPassword())) {
            return;
        }
        user.setEncryptedPassword(draft.getEncryptedPassword());
        user.setPasswordSalt(StringUtils.hasText(draft.getPasswordSalt()) ? draft.getPasswordSalt() : null);
        user.setPasswordEncryptingAlgorithmName(
                StringUtils.hasText(draft.getPasswordAlgorithm()) ? draft.getPasswordAlgorithm() : null);
        Integer iterations = null;
        if (StringUtils.hasText(draft.getPasswordIterations())) {
            try {
                iterations = Integer.valueOf(draft.getPasswordIterations().trim());
            } catch (NumberFormatException ignored) {
                // fall back to the default iterations
            }
        }
        user.setPasswordEncryptingIterations(iterations);
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
