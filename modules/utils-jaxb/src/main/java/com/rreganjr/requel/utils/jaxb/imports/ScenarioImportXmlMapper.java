package com.rreganjr.requel.utils.jaxb.imports;

import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.project.ScenarioImportDraft;
import java.util.HashSet;

public class ScenarioImportXmlMapper {

    public ScenarioImportDraft toDraft(ScenarioImportXml xml) {
        if (xml == null) {
            throw new ImportException("scenario XML payload is required");
        }
        return ScenarioImportDraft.builder()
                .externalId(xml.getId())
                .createdByExternalId(xml.getCreatedBy())
                .name(xml.getName())
                .description(xml.getText())
                .scenarioType(xml.getScenarioType())
                .stepRefs(new HashSet<>(xml.getStepRefs()))
                .annotationExternalIds(new HashSet<>(xml.getAnnotationRefs()))
                .build();
    }
}
