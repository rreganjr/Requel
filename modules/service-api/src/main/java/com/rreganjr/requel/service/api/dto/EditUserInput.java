/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.requel.service.api.dto;

import java.util.Map;
import java.util.Set;

/**
 * Input DTO for the EditUser command. Used for both creating and updating users.
 * When username is provided but no existing user matches, a new user is created.
 * The version field is required for updates to support optimistic locking.
 */
public record EditUserInput(
        Long id,
        Integer version,
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
