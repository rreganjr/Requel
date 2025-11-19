package com.rreganjr.requel.annotation.imports;

import com.rreganjr.requel.annotation.impl.PositionImpl;
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
}
