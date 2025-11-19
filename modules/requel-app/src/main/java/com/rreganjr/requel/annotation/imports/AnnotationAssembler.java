package com.rreganjr.requel.annotation.imports;

import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.impl.IssueImpl;
import com.rreganjr.requel.annotation.impl.NoteImpl;
import com.rreganjr.requel.annotation.impl.PositionImpl;
import com.rreganjr.requel.imports.AggregateAssembler;
import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.ImportUnitOfWork;
import com.rreganjr.requel.imports.annotation.AnnotationImportDraft;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import java.util.Optional;
import org.springframework.util.StringUtils;

public class AnnotationAssembler implements AggregateAssembler<AnnotationImportDraft, Annotation> {

    private final UserRepository userRepository;
    private final User defaultCreatedBy;
    private final Object groupingObject;
    private final AnnotatableResolver annotatableResolver;

    public AnnotationAssembler(UserRepository userRepository, User defaultCreatedBy, Object groupingObject,
                               AnnotatableResolver annotatableResolver) {
        this.userRepository = userRepository;
        this.defaultCreatedBy = defaultCreatedBy;
        this.groupingObject = groupingObject;
        this.annotatableResolver = annotatableResolver;
    }

    @Override
    public Class<AnnotationImportDraft> draftType() {
        return AnnotationImportDraft.class;
    }

    @Override
    public Class<Annotation> aggregateType() {
        return Annotation.class;
    }

    @Override
    public Annotation assemble(AnnotationImportDraft draft, ImportUnitOfWork unitOfWork) throws ImportException {
        if (draft == null) {
            throw new ImportException("annotation draft is required");
        }
        User createdBy = resolveCreatedBy(draft, unitOfWork);
        Annotation annotation;
        if (draft.getType() == AnnotationImportDraft.Type.ISSUE) {
            IssueImpl issue = new IssueImpl(groupingObject, draft.getText(), draft.isMustBeResolved(), createdBy);
            draft.getPositionExternalIds().forEach(posId -> unitOfWork.resolve(PositionImpl.class, posId)
                    .ifPresent(p -> {
                        issue.getPositions().add(p);
                        p.getIssues().add(issue);
                    }));
            attachAnnotatables(issue, draft, unitOfWork);
            annotation = issue;
        } else {
            annotation = new NoteImpl(groupingObject, draft.getText(), createdBy);
            attachAnnotatables(annotation, draft, unitOfWork);
        }
        unitOfWork.register(Annotation.class, draft.getExternalId(), annotation);
        return annotation;
    }

    private User resolveCreatedBy(AnnotationImportDraft draft, ImportUnitOfWork unitOfWork) {
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

    private void attachAnnotatables(Annotation annotation, AnnotationImportDraft draft, ImportUnitOfWork unitOfWork) {
        if (draft.getAnnotatableExternalIds().isEmpty()) {
            return;
        }
        draft.getAnnotatableExternalIds().forEach(id -> annotatableResolver.resolve(draft.getAnnotatableDiscriminator(), id, unitOfWork)
                .ifPresent(annotatable -> {
                    annotation.getAnnotatables().add(annotatable);
                    if (annotatable instanceof com.rreganjr.requel.project.ProjectOrDomainEntity entity) {
                        entity.getAnnotations().add(annotation);
                    }
                }));
    }
}
