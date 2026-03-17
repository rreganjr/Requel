package com.rreganjr.requel.service.api.dto;

/**
 * Lightweight cross-entity pointer. Reused wherever entities reference other
 * entities — goals, actors, stories, containers, referers, etc.
 *
 * @param entityType  simple type name (e.g. "Goal", "Story", "Actor", "Project")
 * @param id          entity id
 * @param name        display name
 */
public record EntityReferenceDto(
        String entityType,
        Long id,
        String name
) {
}
