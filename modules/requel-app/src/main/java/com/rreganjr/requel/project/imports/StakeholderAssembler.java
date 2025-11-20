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
import com.rreganjr.requel.imports.project.StakeholderImportDraft;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.NonUserStakeholderImpl;
import com.rreganjr.requel.project.impl.UserStakeholderImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Assembles user stakeholders from drafts.
 */
public class StakeholderAssembler implements AggregateAssembler<StakeholderImportDraft, com.rreganjr.requel.project.Stakeholder> {

    private final Project project;
    private final UserRepository userRepository;
    private final com.rreganjr.platform.identity.User defaultCreatedBy;

    public StakeholderAssembler(Project project, UserRepository userRepository, com.rreganjr.platform.identity.User defaultCreatedBy) {
        this.project = project;
        this.userRepository = userRepository;
        this.defaultCreatedBy = defaultCreatedBy;
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
            stakeholder = new UserStakeholderImpl(project, createdBy, user);
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
