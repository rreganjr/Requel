package com.rreganjr.requel.service.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Story DTO. The list view omits goals and actors for efficiency;
 * the detail view includes them.
 *
 * @param id          story id
 * @param version     optimistic lock version
 * @param name        story name
 * @param text        story body text
 * @param storyType   "Success" or "Exception"
 * @param createdBy   display name of creator
 * @param goals       associated goals (detail view only)
 * @param actors      associated actors (detail view only)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StoryDto(
        Long id,
        int version,
        String name,
        String text,
        String storyType,
        String createdBy,
        List<EntityReferenceDto> goals,
        List<EntityReferenceDto> actors
) {
}
