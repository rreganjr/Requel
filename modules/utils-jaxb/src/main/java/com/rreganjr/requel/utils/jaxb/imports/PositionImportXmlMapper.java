package com.rreganjr.requel.utils.jaxb.imports;

import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.annotation.PositionImportDraft;
import java.util.HashSet;

public class PositionImportXmlMapper {

    public PositionImportDraft toDraft(PositionImportXml xml) {
        if (xml == null) {
            throw new ImportException("position XML payload is required");
        }
        return PositionImportDraft.builder()
                .externalId(xml.getId())
                .createdByExternalId(xml.getCreatedBy())
                .text(xml.getText())
                .argumentExternalIds(new HashSet<>()) // arguments can be mapped later if needed
                .build();
    }
}
