package com.rreganjr.requel.utils.jaxb.imports;

import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.project.GoalImportDraft;
import java.util.HashSet;
import java.util.Set;

/**
 * Converts goal JAXB DTOs into drafts.
 */
public class GoalImportXmlMapper {

    public GoalImportDraft toDraft(GoalImportXml xml) {
        if (xml == null) {
            throw new ImportException("goal XML payload is required");
        }
        Set<String> relations = new HashSet<>();
        xml.getGoalRelations().forEach(rel -> {
            if ("Supports".equalsIgnoreCase(rel.getRelationType()) && rel.getToGoal() != null) {
                relations.add(rel.getToGoal());
            }
        });

        return GoalImportDraft.builder()
                .externalId(xml.getId())
                .createdByExternalId(xml.getCreatedBy())
                .name(xml.getName())
                .description(xml.getText())
                .relationTargets(relations)
                .annotationExternalIds(new HashSet<>(xml.getAnnotationRefs()))
                .glossaryTermExternalIds(new HashSet<>(xml.getGlossaryTermRefs()))
                .build();
    }
}
