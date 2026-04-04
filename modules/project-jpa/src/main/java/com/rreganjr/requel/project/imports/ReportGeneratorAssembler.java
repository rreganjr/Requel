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

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.imports.AggregateAssembler;
import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.ImportUnitOfWork;
import com.rreganjr.requel.imports.project.ReportGeneratorImportDraft;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.ReportGeneratorImpl;

/**
 * Assembles report generators from import drafts.
 */
public class ReportGeneratorAssembler implements AggregateAssembler<ReportGeneratorImportDraft, ReportGeneratorImpl> {

    private final Project project;
    private final User defaultCreatedBy;

    public ReportGeneratorAssembler(Project project, User defaultCreatedBy) {
        this.project = project;
        this.defaultCreatedBy = defaultCreatedBy;
    }

    @Override
    public Class<ReportGeneratorImportDraft> draftType() {
        return ReportGeneratorImportDraft.class;
    }

    @Override
    public Class<ReportGeneratorImpl> aggregateType() {
        return ReportGeneratorImpl.class;
    }

    @Override
    public ReportGeneratorImpl assemble(ReportGeneratorImportDraft draft, ImportUnitOfWork unitOfWork) throws ImportException {
        if (draft == null) {
            throw new ImportException("report generator draft is required");
        }
        ReportGeneratorImpl report = new ReportGeneratorImpl(project, defaultCreatedBy, draft.getName(), draft.getText());
        project.getReportGenerators().add(report);
        unitOfWork.register(ReportGeneratorImpl.class, draft.getExternalId(), report);
        unitOfWork.register(com.rreganjr.requel.project.ReportGenerator.class, draft.getExternalId(), report);
        return report;
    }
}
