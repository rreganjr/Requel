package com.rreganjr.requel.service.api.dto;

import java.util.Map;
import java.util.Set;

/**
 * Input DTO for the EditUser command. Used for both creating and updating users.
 * When username is provided but no existing user matches, a new user is created.
 */
public record EditUserInput(
        String username,
        String password,
        String repassword,
        String name,
        String emailAddress,
        String phoneNumber,
        String organizationName,
        Boolean editable,
        Set<String> userRoleNames,
        Map<String, Set<String>> userRolePermissionNames
) {
}
