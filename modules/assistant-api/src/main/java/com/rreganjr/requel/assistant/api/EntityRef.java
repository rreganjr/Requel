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
package com.rreganjr.requel.assistant.api;

import java.util.Objects;

/**
 * Stable reference to a Requel entity without exposing a persistence object.
 */
public record EntityRef(String entityType, Long entityId) {

	public EntityRef {
		Objects.requireNonNull(entityType, "entityType");
		Objects.requireNonNull(entityId, "entityId");
	}

	public static EntityRef of(String entityType, Long entityId) {
		return new EntityRef(entityType, entityId);
	}
}
