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
import java.util.Map;

/**
 * Issue #75: what each stakeholder permission brings with it, for the stakeholder page. Keys are
 * the permission keys {@link StakeholderPermissionDto#permissionKey()} carries.
 *
 * @param implied      permissions granted with another: shown checked and disabled, with the
 *                     reason
 * @param ownedDeletes deletes a permission covers for what it owns: the flagged permission gets
 *                     an asterisk and the note
 * @param grantKeys    for each permission, the Grant permission that lets someone give or remove
 *                     it (they must also hold the permission itself)
 */
public record StakeholderPermissionRulesDto(List<Implied> implied, List<OwnedDelete> ownedDeletes,
        Map<String, String> grantKeys) {

    public record Implied(String granted, String implied, String reason) {
    }

    public record OwnedDelete(String granted, String flagged, String note) {
    }
}
