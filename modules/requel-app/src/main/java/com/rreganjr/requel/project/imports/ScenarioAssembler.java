package com.rreganjr.requel.project.imports;

import com.rreganjr.requel.imports.AggregateAssembler;
import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.ImportUnitOfWork;
import com.rreganjr.requel.imports.project.ScenarioImportDraft;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Assembles scenarios/steps from drafts.
 */
public class ScenarioAssembler implements AggregateAssembler<ScenarioImportDraft, ScenarioImpl> {

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
    public Class<ScenarioImpl> aggregateType() {
        return ScenarioImpl.class;
    }

    @Override
    public ScenarioImpl assemble(ScenarioImportDraft draft, ImportUnitOfWork unitOfWork) throws ImportException {
        if (draft == null) {
            throw new ImportException("scenario draft is required");
        }
        User createdBy = resolveCreatedBy(draft, unitOfWork);
        ScenarioType type = ScenarioType.valueOf(draft.getScenarioType());
        ScenarioImpl scenario = new ScenarioImpl(project, createdBy, draft.getName(), draft.getDescription(), type);
        unitOfWork.register(ScenarioImpl.class, draft.getExternalId(), scenario);
        unitOfWork.register(com.rreganjr.requel.project.Scenario.class, draft.getExternalId(), scenario);

        // Step refs are linked via the unit-of-work if present; steps are also scenarios in this model.
        draft.getStepRefs().forEach(stepId -> {
            Optional<ScenarioImpl> step = unitOfWork.resolve(ScenarioImpl.class, stepId);
            step.ifPresent(scenario::addStep);
        });

        return scenario;
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
