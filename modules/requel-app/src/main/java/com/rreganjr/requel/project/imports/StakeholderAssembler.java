package com.rreganjr.requel.project.imports;

import com.rreganjr.requel.imports.AggregateAssembler;
import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.ImportUnitOfWork;
import com.rreganjr.requel.imports.project.StakeholderImportDraft;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.UserStakeholderImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Assembles user stakeholders from drafts.
 */
public class StakeholderAssembler implements AggregateAssembler<StakeholderImportDraft, UserStakeholderImpl> {

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
    public Class<UserStakeholderImpl> aggregateType() {
        return UserStakeholderImpl.class;
    }

    @Override
    public UserStakeholderImpl assemble(StakeholderImportDraft draft, ImportUnitOfWork unitOfWork) throws ImportException {
        if (draft == null) {
            throw new ImportException("stakeholder draft is required");
        }
        User user = resolveUser(draft.getUserExternalId(), unitOfWork);
        com.rreganjr.platform.identity.User createdBy = resolveCreatedBy(draft, unitOfWork);

        UserStakeholderImpl stakeholder = new UserStakeholderImpl(project, createdBy, user);
        unitOfWork.register(UserStakeholderImpl.class, draft.getExternalId(), stakeholder);
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
