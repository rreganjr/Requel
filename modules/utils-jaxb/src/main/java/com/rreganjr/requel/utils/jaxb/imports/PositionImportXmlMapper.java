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
import com.rreganjr.requel.imports.annotation.PositionImportDraft;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class PositionImportXmlMapper {

    public PositionImportDraft toDraft(PositionImportXml xml) {
        if (xml == null) {
            throw new ImportException("position XML payload is required");
        }
        List<PositionImportDraft.ArgumentDraft> argumentDrafts = xml.getArguments().stream()
                .map(arg -> new PositionImportDraft.ArgumentDraft(arg.getId(), arg.getCreatedBy(),
                        arg.getText(), arg.getSupportLevel()))
                .collect(Collectors.toList());
        String text = xml.getText();
        if (text == null || text.isBlank()) {
            text = xml.getProposedWord(); // fallback for changeSpellingPosition
        }
        return PositionImportDraft.builder()
                .externalId(xml.getId())
                .createdByExternalId(xml.getCreatedBy())
                .text(text)
                .argumentExternalIds(new HashSet<>())
                .arguments(argumentDrafts)
                .build();
    }
}
