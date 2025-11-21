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
package com.rreganjr.requel.project.imports;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.imports.AggregateAssembler;
import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.ImportUnitOfWork;
import com.rreganjr.requel.imports.project.GlossaryTermImportDraft;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.GlossaryTermImpl;
import com.rreganjr.requel.user.UserRepository;
import java.util.Optional;
import org.springframework.util.StringUtils;

public class GlossaryTermAssembler implements AggregateAssembler<GlossaryTermImportDraft, GlossaryTermImpl> {

    private final Project project;
    private final UserRepository userRepository;
    private final User defaultCreatedBy;

    public GlossaryTermAssembler(Project project, UserRepository userRepository, User defaultCreatedBy) {
        this.project = project;
        this.userRepository = userRepository;
        this.defaultCreatedBy = defaultCreatedBy;
    }

    @Override
    public Class<GlossaryTermImportDraft> draftType() {
        return GlossaryTermImportDraft.class;
    }

    @Override
    public Class<GlossaryTermImpl> aggregateType() {
        return GlossaryTermImpl.class;
    }

    @Override
    public GlossaryTermImpl assemble(GlossaryTermImportDraft draft, ImportUnitOfWork unitOfWork) throws ImportException {
        if (draft == null) {
            throw new ImportException("glossary term draft is required");
        }
        User createdBy = resolveCreatedBy(draft, unitOfWork);
        GlossaryTermImpl term = new GlossaryTermImpl(project, draft.getName(), createdBy);
        term.setText(draft.getText());
        project.getGlossaryTerms().add(term);
        unitOfWork.register(GlossaryTermImpl.class, draft.getExternalId(), term);
        unitOfWork.register(com.rreganjr.requel.project.GlossaryTerm.class, draft.getExternalId(), term);
        return term;
    }

    public boolean attachCanonicalTerm(GlossaryTermImportDraft draft, ImportUnitOfWork unitOfWork) {
        if (!StringUtils.hasText(draft.getCanonicalTermExternalId())) {
            return true;
        }
        Optional<GlossaryTermImpl> term = unitOfWork.resolve(GlossaryTermImpl.class, draft.getExternalId());
        Optional<GlossaryTermImpl> canonical = unitOfWork.resolve(GlossaryTermImpl.class, draft.getCanonicalTermExternalId());
        if (term.isPresent() && canonical.isPresent()) {
            GlossaryTermImpl alias = term.get();
            GlossaryTermImpl canonicalTerm = canonical.get();
            if (alias != canonicalTerm) {
                alias.setCanonicalTerm(canonicalTerm);
                canonicalTerm.getAlternateTerms().add(alias);
            }
            return true;
        }
        return false;
    }

    private User resolveCreatedBy(GlossaryTermImportDraft draft, ImportUnitOfWork unitOfWork) {
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
