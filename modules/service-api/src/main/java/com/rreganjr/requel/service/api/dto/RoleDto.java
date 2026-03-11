package com.rreganjr.requel.service.api.dto;

import java.util.List;

/**
 * Role definition with its available permissions.
 * Used by the user editor to populate the role/permission checkboxes.
 */
public record RoleDto(
        String roleName,
        String displayName,
        List<PermissionDto> availablePermissions
) {
    public record PermissionDto(String name) {}
}
