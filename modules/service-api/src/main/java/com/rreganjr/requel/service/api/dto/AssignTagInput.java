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
 * Input for AssignTag: attach tag {@code tagId} to the entity identified by
 * {@code entityType} (registry discriminator, e.g. "Goal") and {@code entityId}.
 */
@CommandDescription(value = "Attaches an existing tag (tagId) to the entity given by entityType"
        + " and entityId. entityType is case-sensitive: Goal, Project, Actor, Story, Scenario,"
        + " UseCase, NonUserStakeholder or UserStakeholder. Attaching a tag the entity already"
        + " carries changes nothing. When the tag's category has rules, looked up in the project"
        + " first and then globally, its allowed entity types are enforced, and an exclusive"
        + " category replaces any other tag of that category on the entity. The response is the"
        + " tag, not the entity.",
        authorization = "Annotation[Edit] on the tagged entity's project")
public record AssignTagInput(
        Long tagId,
        String entityType,
        Long entityId
) {
}
