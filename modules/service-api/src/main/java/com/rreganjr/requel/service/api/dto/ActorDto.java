package com.rreganjr.requel.service.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Actor DTO. The list view omits goals and referencedBy for efficiency;
 * the detail view includes them.
 *
 * @param id                   actor id
 * @param version              optimistic lock version
 * @param name                 actor name
 * @param text                 actor description/body text
 * @param createdBy            display name of creator
 * @param goals                associated goals (detail view only)
 * @param referencedByUseCases use cases that reference this actor (detail view only)
 * @param referencedByStories  stories that reference this actor (detail view only)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActorDto(
        Long id,
        int version,
        String name,
        String text,
        String createdBy,
        List<EntityReferenceDto> goals,
        List<EntityReferenceDto> referencedByUseCases,
        List<EntityReferenceDto> referencedByStories
) {
}
