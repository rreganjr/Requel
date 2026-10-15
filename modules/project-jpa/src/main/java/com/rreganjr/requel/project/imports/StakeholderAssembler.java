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
import com.rreganjr.requel.imports.project.StakeholderImportDraft;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.impl.NonUserStakeholderImpl;
import com.rreganjr.requel.project.impl.UserStakeholderImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Assembles user stakeholders from drafts.
 */
public class StakeholderAssembler implements AggregateAssembler<StakeholderImportDraft, com.rreganjr.requel.project.Stakeholder> {

    private final Project project;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(StakeholderAssembler.class);

    private final com.rreganjr.platform.identity.User defaultCreatedBy;
    /** The grantable permissions by permission key, to give user stakeholders theirs back (#398). */
    private final Map<String, StakeholderPermission> permissionsByKey;

    public StakeholderAssembler(Project project, UserRepository userRepository, com.rreganjr.platform.identity.User defaultCreatedBy) {
        this(project, userRepository, defaultCreatedBy, Collections.emptySet());
    }

    /**
     * @param availablePermissions the project permissions that can be granted (the repository's
     *            {@code findAvailableStakeholderPermissions()}); an imported user stakeholder gets
     *            the ones its XML lists.
     */
    public StakeholderAssembler(Project project, UserRepository userRepository, com.rreganjr.platform.identity.User defaultCreatedBy,
            Collection<StakeholderPermission> availablePermissions) {
        this.project = project;
        this.userRepository = userRepository;
        this.defaultCreatedBy = defaultCreatedBy;
        this.permissionsByKey = availablePermissions.stream()
                .collect(Collectors.toMap(StakeholderPermission::getPermissionKey, Function.identity(), (a, b) -> a));
    }

    @Override
    public Class<StakeholderImportDraft> draftType() {
        return StakeholderImportDraft.class;
    }

    @Override
    public Class<com.rreganjr.requel.project.Stakeholder> aggregateType() {
        return com.rreganjr.requel.project.Stakeholder.class;
    }

    @Override
    public com.rreganjr.requel.project.Stakeholder assemble(StakeholderImportDraft draft, ImportUnitOfWork unitOfWork) throws ImportException {
        if (draft == null) {
            throw new ImportException("stakeholder draft is required");
        }
        com.rreganjr.platform.identity.User createdBy = resolveCreatedBy(draft, unitOfWork);
        com.rreganjr.requel.project.Stakeholder stakeholder;
        if (draft.isUserStakeholder()) {
            User user = resolveUser(draft.getUserExternalId(), unitOfWork);
            UserStakeholderImpl userStakeholder = new UserStakeholderImpl(project, createdBy, user);
            grantPermissions(userStakeholder, draft);
            stakeholder = userStakeholder;
        } else {
            stakeholder = new NonUserStakeholderImpl(project, createdBy, draft.getName());
            ((NonUserStakeholderImpl) stakeholder).setText(draft.getText());
        }
        Class<?> registrationType = (stakeholder instanceof UserStakeholderImpl)
                ? UserStakeholderImpl.class
                : NonUserStakeholderImpl.class;
        unitOfWork.register(com.rreganjr.requel.project.Stakeholder.class, draft.getExternalId(), stakeholder);
        return stakeholder;
    }

    /** #398: before this, an imported user stakeholder came back with no permissions. */
    private void grantPermissions(UserStakeholderImpl stakeholder, StakeholderImportDraft draft) {
        for (String key : draft.getPermissionKeys()) {
            StakeholderPermission permission = permissionsByKey.get(key);
            if (permission == null) {
                log.warn("Skipping unknown project permission {} for imported stakeholder {}", key,
                        draft.getExternalId());
                continue;
            }
            stakeholder.grantStakeholderPermission(permission);
        }
    }

    private User resolveUser(String externalId, ImportUnitOfWork unitOfWork) {
        Optional<User> u = unitOfWork.resolve(User.class, externalId);
        if (u.isPresent()) {
            return u.get();
        }
        try {
            return (User) userRepository.findUserByUsername(externalId);
        } catch (Exception e) {
            throw new ImportException("Unable to resolve user for stakeholder: " + externalId, e);
        }
    }

    private com.rreganjr.platform.identity.User resolveCreatedBy(StakeholderImportDraft draft, ImportUnitOfWork unitOfWork) {
        if (StringUtils.hasText(draft.getCreatedByExternalId())) {
            Optional<User> resolved = unitOfWork.resolve(User.class, draft.getCreatedByExternalId());
            if (resolved.isPresent()) {
                return resolved.get();
            }
        }
        return defaultCreatedBy;
    }
}
