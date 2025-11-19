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
                .annotatableDiscriminator(xml.getAnnotatableDiscriminator())
                .build();
    }
}
