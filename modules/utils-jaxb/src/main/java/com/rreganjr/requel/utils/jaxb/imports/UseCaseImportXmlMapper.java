package com.rreganjr.requel.utils.jaxb.imports;

import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.project.UseCaseImportDraft;
import java.util.HashSet;

public class UseCaseImportXmlMapper {

    public UseCaseImportDraft toDraft(UseCaseImportXml xml) {
        if (xml == null) {
            throw new ImportException("use case XML payload is required");
        }
        return UseCaseImportDraft.builder()
                .externalId(xml.getId())
                .createdByExternalId(xml.getCreatedBy())
                .name(xml.getName())
                .description(xml.getText())
                .primaryActorExternalId(xml.getPrimaryActorRef())
                .scenarioExternalId(xml.getScenarioRef())
                .storyExternalIds(new HashSet<>(xml.getStoryRefs()))
                .goalExternalIds(new HashSet<>(xml.getGoalRefs()))
                .actorExternalIds(new HashSet<>(xml.getActorRefs()))
                .annotationExternalIds(new HashSet<>(xml.getAnnotationRefs()))
                .build();
    }
}
