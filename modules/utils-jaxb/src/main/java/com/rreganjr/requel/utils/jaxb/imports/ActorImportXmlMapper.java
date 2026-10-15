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
import com.rreganjr.requel.imports.project.ActorImportDraft;
import java.util.HashSet;
import java.util.Set;

/**
 * Converts JAXB DTOs into format-agnostic drafts.
 */
public class ActorImportXmlMapper {

    public ActorImportDraft toDraft(ActorImportXml xml) {
        if (xml == null) {
            throw new ImportException("actor XML payload is required");
        }
        Set<String> annotationRefs = new HashSet<>(xml.getAnnotationRefs());
        Set<String> goalRefs = new HashSet<>(xml.getGoalRefs());
        Set<String> glossaryRefs = new HashSet<>(xml.getGlossaryTermRefs());

        return ActorImportDraft.builder()
                .externalId(xml.getId())
                .createdByExternalId(xml.getCreatedBy())
                .name(xml.getName())
                .description(xml.getText())
                .annotationExternalIds(annotationRefs)
                .goalExternalIds(goalRefs)
                .glossaryTermExternalIds(glossaryRefs)
                .build();
    }
}
