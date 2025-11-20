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
        return PositionImportDraft.builder()
                .externalId(xml.getId())
                .createdByExternalId(xml.getCreatedBy())
                .text(xml.getText())
                .argumentExternalIds(new HashSet<>())
                .arguments(argumentDrafts)
                .build();
    }
}
