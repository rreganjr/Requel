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
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Assembles goals from drafts and wires up support relations within the unit-of-work.
 */
public class GoalAssembler implements AggregateAssembler<GoalImportDraft, GoalImpl> {

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

        unitOfWork.register(GoalImpl.class, draft.getExternalId(), goal);
        unitOfWork.register(Goal.class, draft.getExternalId(), goal);

        return goal;
    }

    /**
     * Attach support relations for the given draft after all goals are registered.
     */
    public void attachSupports(GoalImportDraft draft, ImportUnitOfWork unitOfWork) {
        draft.getRelationTargets().forEach(targetId -> {
            Optional<Goal> sourceOpt = unitOfWork.resolve(Goal.class, draft.getExternalId());
            Optional<Goal> targetOpt = unitOfWork.resolve(Goal.class, targetId);
            if (sourceOpt.isPresent() && targetOpt.isPresent()) {
                linkSupport((GoalImpl) sourceOpt.get(), targetOpt.get());
            }
        });
    }

    private void linkSupport(GoalImpl source, Goal target) {
        GoalRelationImpl relation = new GoalRelationImpl(source, target, GoalRelationType.Supports, defaultCreatedBy);
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
}
