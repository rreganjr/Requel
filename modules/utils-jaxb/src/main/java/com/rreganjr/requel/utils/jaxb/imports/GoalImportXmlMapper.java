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
package com.rreganjr.requel.utils.jaxb.imports;

import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.project.GoalImportDraft;
import java.util.HashSet;

/**
 * Converts goal JAXB DTOs into drafts.
 */
public class GoalImportXmlMapper {

    public GoalImportDraft toDraft(GoalImportXml xml) {
        if (xml == null) {
            throw new ImportException("goal XML payload is required");
        }
        GoalImportDraft.Builder builder = GoalImportDraft.builder();
        // Every relation type, not only Supports: the type is carried as written and the
        // assembler parses it, skipping an unknown value with a WARN (issue #257).
        xml.getGoalRelations().forEach(rel -> {
            if (rel.getToGoal() != null) {
                builder.relation(rel.getToGoal(), rel.getRelationType());
            }
        });

        return builder
                .externalId(xml.getId())
                .createdByExternalId(xml.getCreatedBy())
                .name(xml.getName())
                .description(xml.getText())
                .annotationExternalIds(new HashSet<>(xml.getAnnotationRefs()))
                .glossaryTermExternalIds(new HashSet<>(xml.getGlossaryTermRefs()))
                .build();
    }
}
