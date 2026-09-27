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
import com.rreganjr.requel.imports.project.GoalImportDraft;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalRelationType;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.GoalRelationImpl;
import com.rreganjr.requel.project.impl.GlossaryTermImpl;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Assembles goals from drafts and wires up goal relations within the unit-of-work.
 */
public class GoalAssembler implements AggregateAssembler<GoalImportDraft, GoalImpl> {

    private static final Logger log = LoggerFactory.getLogger(GoalAssembler.class);

    private final Project project;
    private final UserRepository userRepository;
    private final User defaultCreatedBy;

    public GoalAssembler(Project project, UserRepository userRepository, User defaultCreatedBy) {
        this.project = project;
        this.userRepository = userRepository;
        this.defaultCreatedBy = defaultCreatedBy;
    }

    @Override
    public Class<GoalImportDraft> draftType() {
        return GoalImportDraft.class;
    }

    @Override
    public Class<GoalImpl> aggregateType() {
        return GoalImpl.class;
    }

    @Override
    public GoalImpl assemble(GoalImportDraft draft, ImportUnitOfWork unitOfWork) throws ImportException {
        if (draft == null) {
            throw new ImportException("goal draft is required");
        }

        User createdBy = resolveCreatedBy(draft, unitOfWork);
        GoalImpl goal = new GoalImpl(project, createdBy, draft.getName(), draft.getDescription());
        attachGlossaryTerms(goal, draft.getGlossaryTermExternalIds(), unitOfWork);

        unitOfWork.register(GoalImpl.class, draft.getExternalId(), goal);
        unitOfWork.register(Goal.class, draft.getExternalId(), goal);

        return goal;
    }

    /**
     * Attach the draft's goal relations after all goals are registered. A relation whose type
     * this build does not know (a newer export, or a hand edit) is skipped with a WARN and the
     * rest of the file imports; one whose target is not in the file is skipped as before.
     * Issue #257.
     */
    public void attachRelations(GoalImportDraft draft, ImportUnitOfWork unitOfWork) {
        draft.getRelations().forEach((targetId, typeName) -> {
            Optional<Goal> sourceOpt = unitOfWork.resolve(Goal.class, draft.getExternalId());
            Optional<Goal> targetOpt = unitOfWork.resolve(Goal.class, targetId);
            if (sourceOpt.isEmpty() || targetOpt.isEmpty()) {
                return;
            }
            Optional<GoalRelationType> type = GoalRelationType.parse(typeName);
            if (type.isEmpty()) {
                log.warn("import: skipping goal relation \"{}\" -> \"{}\": unknown relationType '{}'",
                        sourceOpt.get().getName(), targetOpt.get().getName(), typeName);
                return;
            }
            link((GoalImpl) sourceOpt.get(), targetOpt.get(), type.get());
        });
    }

    private void link(GoalImpl source, Goal target, GoalRelationType type) {
        GoalRelationImpl relation = new GoalRelationImpl(source, target, type, defaultCreatedBy);
        source.getRelationsFromThisGoal().add(relation);
        target.getRelationsToThisGoal().add(relation);
    }

    private User resolveCreatedBy(GoalImportDraft draft, ImportUnitOfWork unitOfWork) {
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

    private void attachGlossaryTerms(GoalImpl goal, java.util.Set<String> termIds, ImportUnitOfWork unitOfWork) {
        termIds.forEach(termId -> unitOfWork.resolve(GlossaryTermImpl.class, termId)
                .ifPresent(term -> {
                    goal.getGlossaryTerms().add(term);
                    term.getReferers().add(goal);
                }));
    }
}
