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
import com.rreganjr.requel.imports.project.ScenarioImportDraft;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.impl.StepImpl;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Assembles scenarios/steps from drafts.
 */
public class ScenarioAssembler implements AggregateAssembler<ScenarioImportDraft, StepImpl> {

    private final Project project;
    private final UserRepository userRepository;
    private final User defaultCreatedBy;

    public ScenarioAssembler(Project project, UserRepository userRepository, User defaultCreatedBy) {
        this.project = project;
        this.userRepository = userRepository;
        this.defaultCreatedBy = defaultCreatedBy;
    }

    @Override
    public Class<ScenarioImportDraft> draftType() {
        return ScenarioImportDraft.class;
    }

    @Override
    public Class<StepImpl> aggregateType() {
        return StepImpl.class;
    }

    @Override
    public StepImpl assemble(ScenarioImportDraft draft, ImportUnitOfWork unitOfWork) throws ImportException {
        if (draft == null) {
            throw new ImportException("scenario draft is required");
        }
        User createdBy = resolveCreatedBy(draft, unitOfWork);
        ScenarioType type;
        try {
            type = ScenarioType.valueOf(draft.getScenarioType());
        } catch (Exception e) {
            // treat step defaults as Primary
            type = ScenarioType.Primary;
        }
        StepImpl step;
        if (draft.isScenarioElement()) {
            ScenarioImpl scenario = new ScenarioImpl(project, createdBy, draft.getName(), draft.getDescription(), type);
            unitOfWork.register(ScenarioImpl.class, draft.getExternalId(), scenario);
            unitOfWork.register(com.rreganjr.requel.project.Scenario.class, draft.getExternalId(), scenario);
            step = scenario;
        } else {
            step = new StepImpl(project, createdBy, draft.getName(), draft.getDescription(), type);
        }
        unitOfWork.register(StepImpl.class, draft.getExternalId(), step);
        unitOfWork.register(com.rreganjr.requel.project.Step.class, draft.getExternalId(), step);
        return step;
    }

    public void attachSteps(ScenarioImportDraft draft, ImportUnitOfWork unitOfWork) {
        unitOfWork.resolve(ScenarioImpl.class, draft.getExternalId()).ifPresent(scenario -> {
            draft.getStepRefs().forEach(stepId -> unitOfWork.resolve(StepImpl.class, stepId)
                    .ifPresent(scenario::addStep));
        });
    }

    private User resolveCreatedBy(ScenarioImportDraft draft, ImportUnitOfWork unitOfWork) {
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
}
