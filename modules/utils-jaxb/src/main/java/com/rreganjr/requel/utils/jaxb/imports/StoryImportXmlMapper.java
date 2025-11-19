package com.rreganjr.requel.utils.jaxb.imports;

import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.project.StoryImportDraft;
import java.util.HashSet;

public class StoryImportXmlMapper {

    public StoryImportDraft toDraft(StoryImportXml xml) {
        if (xml == null) {
            throw new ImportException("story XML payload is required");
        }

        return StoryImportDraft.builder()
                .externalId(xml.getId())
                .createdByExternalId(xml.getCreatedBy())
                .name(xml.getName())
                .description(xml.getText())
                .storyType(xml.getStoryType())
                .goalExternalIds(new HashSet<>(xml.getGoalRefs()))
                .actorExternalIds(new HashSet<>(xml.getActorRefs()))
                .annotationExternalIds(new HashSet<>(xml.getAnnotationRefs()))
                .glossaryTermExternalIds(new HashSet<>(xml.getGlossaryTermRefs()))
                .build();
    }
}
