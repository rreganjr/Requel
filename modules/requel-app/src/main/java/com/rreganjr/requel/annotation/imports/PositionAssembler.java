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
package com.rreganjr.requel.annotation.imports;

import com.rreganjr.requel.annotation.impl.PositionImpl;
import com.rreganjr.requel.annotation.impl.ArgumentImpl;
import com.rreganjr.requel.annotation.ArgumentPositionSupportLevel;
import com.rreganjr.requel.imports.AggregateAssembler;
import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.ImportUnitOfWork;
import com.rreganjr.requel.imports.annotation.PositionImportDraft;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import java.util.Optional;
import org.springframework.util.StringUtils;

public class PositionAssembler implements AggregateAssembler<PositionImportDraft, PositionImpl> {

    private final UserRepository userRepository;
    private final User defaultCreatedBy;

    public PositionAssembler(UserRepository userRepository, User defaultCreatedBy) {
        this.userRepository = userRepository;
        this.defaultCreatedBy = defaultCreatedBy;
    }

    @Override
    public Class<PositionImportDraft> draftType() {
        return PositionImportDraft.class;
    }

    @Override
    public Class<PositionImpl> aggregateType() {
        return PositionImpl.class;
    }

    @Override
    public PositionImpl assemble(PositionImportDraft draft, ImportUnitOfWork unitOfWork) throws ImportException {
        if (draft == null) {
            throw new ImportException("position draft is required");
        }
        User createdBy = resolveCreatedBy(draft, unitOfWork);
        PositionImpl position = new PositionImpl(draft.getText(), createdBy);
        unitOfWork.register(PositionImpl.class, draft.getExternalId(), position);
        unitOfWork.register(com.rreganjr.requel.annotation.Position.class, draft.getExternalId(), position);
        draft.getArguments().forEach(argDraft -> {
            User argCreatedBy = resolveUser(argDraft.getCreatedByExternalId(), unitOfWork);
            ArgumentPositionSupportLevel level = parseSupportLevel(argDraft.getSupportLevel());
            ArgumentImpl argument = new ArgumentImpl(position, argDraft.getText(), level, argCreatedBy);
            position.getArguments().add(argument);
        });
        return position;
    }

    private User resolveCreatedBy(PositionImportDraft draft, ImportUnitOfWork unitOfWork) {
        if (StringUtils.hasText(draft.getCreatedByExternalId())) {
            Optional<User> resolved = unitOfWork.resolve(User.class, draft.getCreatedByExternalId());
            if (resolved.isPresent()) {
                return resolved.get();
            }
            try {
                return userRepository.findUserByUsername(draft.getCreatedByExternalId());
            } catch (Exception ignored) {
            }
        }
        return defaultCreatedBy;
    }

    private User resolveUser(String externalId, ImportUnitOfWork unitOfWork) {
        if (StringUtils.hasText(externalId)) {
            Optional<User> resolved = unitOfWork.resolve(User.class, externalId);
            if (resolved.isPresent()) {
                return resolved.get();
            }
            try {
                return userRepository.findUserByUsername(externalId);
            } catch (Exception ignored) {
            }
        }
        return defaultCreatedBy;
    }

    private ArgumentPositionSupportLevel parseSupportLevel(String value) {
        if (!StringUtils.hasText(value)) {
            return ArgumentPositionSupportLevel.Neutral;
        }
        try {
            return ArgumentPositionSupportLevel.valueOf(value);
        } catch (IllegalArgumentException e) {
            return ArgumentPositionSupportLevel.Neutral;
        }
    }
}
