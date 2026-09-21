/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
import com.rreganjr.requel.imports.project.DictionaryWordImportDraft;

public class DictionaryWordImportXmlMapper {

    public DictionaryWordImportDraft toDraft(DictionaryWordImportXml xml) {
        if (xml == null) {
            throw new ImportException("dictionary word XML payload is required");
        }
        return DictionaryWordImportDraft.builder()
                .lemma(xml.getLemma())
                .phoneticCode(xml.getPhoneticCode())
                .build();
    }
}
