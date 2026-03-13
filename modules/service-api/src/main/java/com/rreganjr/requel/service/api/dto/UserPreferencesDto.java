package com.rreganjr.requel.service.api.dto;

/**
 * DTO for user UI preferences. Separate from UserDto — preferences are
 * a different concern from identity/auth/contact info.
 */
public record UserPreferencesDto(
        int sidebarProjectLimit,
        String sidebarProjectStaleness
) {}
