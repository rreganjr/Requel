package com.rreganjr.requel.service.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Goal DTO. The list view omits relations and referencedBy for efficiency;
 * the detail view includes them.
 *
 * @param id                     goal id
 * @param version                optimistic lock version
 * @param name                   goal name
 * @param text                   goal description/body text
 * @param createdBy              display name of creator
 * @param relationsFromThisGoal  outgoing relations (detail view only)
 * @param relationsToThisGoal    incoming relations (detail view only)
 * @param referencedBy           containers that reference this goal (detail view only)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoalDto(
        Long id,
        int version,
        String name,
        String text,
        String createdBy,
        List<GoalRelationDto> relationsFromThisGoal,
        List<GoalRelationDto> relationsToThisGoal,
        List<EntityReferenceDto> referencedBy
) {
}
