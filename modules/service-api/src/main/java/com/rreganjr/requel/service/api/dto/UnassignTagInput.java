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

import com.rreganjr.requel.service.api.CommandDescription;
/**
 * Input for UnassignTag: detach tag {@code tagId} from the entity identified by
 * {@code entityType} (registry discriminator) and {@code entityId}.
 */
@CommandDescription(value = "Detaches a tag (tagId) from the entity given by entityType and"
        + " entityId. entityType is case-sensitive: Goal, Project, Actor, Story, Scenario, UseCase,"
        + " NonUserStakeholder or UserStakeholder. The tag itself is kept, and detaching a tag the"
        + " entity does not carry changes nothing.",
        authorization = "Annotation[Edit] on the tagged entity's project")
public record UnassignTagInput(
        Long tagId,
        String entityType,
        Long entityId
) {
}
