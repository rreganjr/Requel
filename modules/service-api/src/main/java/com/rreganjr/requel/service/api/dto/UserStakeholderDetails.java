package com.rreganjr.requel.service.api.dto;

import java.util.List;

/**
 * Type-specific details for a user-backed stakeholder.
 * All fields are non-null when present — the null boundary is
 * at the StakeholderDto.userDetails level, not per-field.
 */
public record UserStakeholderDetails(
        String username,
        String emailAddress,
        String phoneNumber,
        String teamName,
        List<String> permissionKeys
) {
}
