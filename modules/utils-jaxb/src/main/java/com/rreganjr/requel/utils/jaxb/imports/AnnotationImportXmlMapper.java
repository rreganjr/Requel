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
package com.rreganjr.requel.utils.jaxb.imports;

import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.annotation.AnnotationImportDraft;
import java.util.HashSet;

public class AnnotationImportXmlMapper {

    public AnnotationImportDraft toDraft(AnnotationImportXml xml, AnnotationImportDraft.Type type) {
        if (xml == null) {
            throw new ImportException("annotation XML payload is required");
        }
        boolean mustBeResolved = xml.getMustBeResolved() != null && xml.getMustBeResolved();
        return AnnotationImportDraft.builder()
                .externalId(xml.getId())
                .createdByExternalId(xml.getCreatedBy())
                .text(xml.getText())
                .type(type)
                .mustBeResolved(mustBeResolved)
                .positionExternalIds(new HashSet<>(xml.getPositionRefs()))
                .annotatableExternalIds(new HashSet<>(xml.getAnnotatableRefs()))
                .build();
    }
}
