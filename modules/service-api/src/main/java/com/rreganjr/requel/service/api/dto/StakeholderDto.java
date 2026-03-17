package com.rreganjr.requel.service.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Unified DTO for both user and non-user stakeholders.
 * Uses nested type-specific objects rather than flat nullable fields —
 * see UI_DESIGN_GUIDE.md §14 "Polymorphic DTOs" for rationale.
 *
 * @param id                  stakeholder id
 * @param version             optimistic lock version
 * @param name                display name
 * @param type                "user" or "non-user"
 * @param createdBy           display name of the user who created this stakeholder
 * @param userDetails         non-null for user stakeholders; null for non-user
 * @param nonUserDetails      non-null for non-user stakeholders; null for user
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StakeholderDto(
        Long id,
        int version,
        String name,
        String type,
        String createdBy,
        UserStakeholderDetails userDetails,
        NonUserStakeholderDetails nonUserDetails
) {
}
