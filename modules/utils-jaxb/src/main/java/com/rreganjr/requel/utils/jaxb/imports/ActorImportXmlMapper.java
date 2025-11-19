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
