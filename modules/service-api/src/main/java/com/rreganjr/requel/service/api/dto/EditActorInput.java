package com.rreganjr.requel.service.api.dto;

/**
 * Input for EditActor command.
 * actorId is null for create, non-null for update.
 * version is null for create, required for update (optimistic lock check).
 */
public record EditActorInput(
        String projectName,
        Long actorId,
        String name,
        String description,
        Integer version
) {
}
