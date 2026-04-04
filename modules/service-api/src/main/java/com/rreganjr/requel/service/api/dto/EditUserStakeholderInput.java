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

import java.util.List;

/**
 * Input DTO for creating or editing a user-backed stakeholder.
 *
 * @param projectName     project to add the stakeholder to
 * @param username        system user to link as a stakeholder
 * @param teamName        optional team assignment (find-or-create)
 * @param permissionKeys  stakeholder permission keys (e.g. "com.rreganjr.requel.project.Goal[Edit]")
 * @param version         optimistic lock version (null for create)
 */
public record EditUserStakeholderInput(
        String projectName,
        String username,
        String teamName,
        List<String> permissionKeys,
        Integer version
) {
}
