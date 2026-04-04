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
package com.rreganjr.requel.annotation.imports;

import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.impl.IssueImpl;
import com.rreganjr.requel.annotation.impl.NoteImpl;
import com.rreganjr.requel.annotation.impl.PositionImpl;
import com.rreganjr.requel.imports.AggregateAssembler;
import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.ImportUnitOfWork;
import com.rreganjr.requel.imports.annotation.AnnotationImportDraft;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.util.StringUtils;

public class AnnotationAssembler implements AggregateAssembler<AnnotationImportDraft, Annotation> {

    private final UserRepository userRepository;
    private final User defaultCreatedBy;
    private final Object groupingObject;
    private final AnnotationLinkRegistry linkRegistry;

    public AnnotationAssembler(UserRepository userRepository, User defaultCreatedBy, Object groupingObject,
                               AnnotationLinkRegistry linkRegistry) {
        this.userRepository = userRepository;
        this.defaultCreatedBy = defaultCreatedBy;
        this.groupingObject = groupingObject;
        this.linkRegistry = linkRegistry;
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
        if (draft.getType() == AnnotationImportDraft.Type.ISSUE
                || draft.getType() == AnnotationImportDraft.Type.LEXICAL_ISSUE) {
            IssueImpl issue;
            if (draft.getType() == AnnotationImportDraft.Type.LEXICAL_ISSUE) {
                issue = new com.rreganjr.requel.annotation.impl.LexicalIssue(groupingObject, draft.getText(),
                        draft.isMustBeResolved(), createdBy, draft.getAnnotatablePropertyName(), draft.getWord());
            } else {
                issue = new IssueImpl(groupingObject, draft.getText(), draft.isMustBeResolved(), createdBy);
            }
            draft.getPositionExternalIds().forEach(posId -> unitOfWork.resolve(PositionImpl.class, posId)
                    .ifPresent(p -> {
                        issue.getPositions().add(p);
                        p.getIssues().add(issue);
                    }));
            attachAnnotatables(issue, draft);
            annotation = issue;
        } else {
            annotation = new NoteImpl(groupingObject, draft.getText(), createdBy);
            attachAnnotatables(annotation, draft);
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

    private void attachAnnotatables(Annotation annotation, AnnotationImportDraft draft) {
        List<com.rreganjr.requel.annotation.Annotatable> annotatables = linkRegistry.consumeLinks(draft.getExternalId());
        annotatables.forEach(annotatable -> {
            annotation.getAnnotatables().add(annotatable);
            annotatable.getAnnotations().add(annotation);
        });
    }
}
