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
import com.rreganjr.requel.imports.project.ActorImportDraft;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.GlossaryTermImpl;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Assembles actors from import drafts, resolving cross references through the unit of work.
 */
public class ActorAssembler implements AggregateAssembler<ActorImportDraft, ActorImpl> {

    private final Project project;
    private final UserRepository userRepository;
    private final User defaultCreatedBy;

    public ActorAssembler(Project project, UserRepository userRepository, User defaultCreatedBy) {
        this.project = project;
        this.userRepository = userRepository;
        this.defaultCreatedBy = defaultCreatedBy;
    }

    @Override
    public Class<ActorImportDraft> draftType() {
        return ActorImportDraft.class;
    }

    @Override
    public Class<ActorImpl> aggregateType() {
        return ActorImpl.class;
    }

    @Override
    public ActorImpl assemble(ActorImportDraft draft, ImportUnitOfWork unitOfWork) throws ImportException {
        if (draft == null) {
            throw new ImportException("actor draft is required");
        }

        User createdBy = resolveCreatedBy(draft, unitOfWork);
        ActorImpl actor = new ActorImpl(project, createdBy, draft.getName(), draft.getDescription());

        // Resolve goal references lazily via the unit-of-work cache.
        draft.getGoalExternalIds().forEach(goalId -> {
            Optional<Goal> goal = unitOfWork.resolve(Goal.class, goalId);
            goal.ifPresent(resolved -> {
                actor.getGoals().add(resolved);
                resolved.getReferers().add(actor);
            });
        });

        attachGlossaryTerms(actor, draft.getGlossaryTermExternalIds(), unitOfWork);

        // Cache the assembled actor for later references (e.g., use cases)
        unitOfWork.register(ActorImpl.class, draft.getExternalId(), actor);
        unitOfWork.register(com.rreganjr.requel.project.Actor.class, draft.getExternalId(), actor);
        return actor;
    }

    private User resolveCreatedBy(ActorImportDraft draft, ImportUnitOfWork unitOfWork) {
        if (StringUtils.hasText(draft.getCreatedByExternalId())) {
            Optional<User> resolved = unitOfWork.resolve(User.class, draft.getCreatedByExternalId());
            if (resolved.isPresent()) {
                return resolved.get();
            }
            try {
                return userRepository.findUserByUsername(draft.getCreatedByExternalId());
            } catch (Exception ignored) {
                // fallback to default
            }
        }
        return defaultCreatedBy;
    }

    private void attachGlossaryTerms(ActorImpl actor, java.util.Set<String> termIds, ImportUnitOfWork unitOfWork) {
        termIds.forEach(termId -> unitOfWork.resolve(GlossaryTermImpl.class, termId)
                .ifPresent(term -> {
                    actor.getGlossaryTerms().add(term);
                    term.getReferers().add(actor);
                }));
    }
}
