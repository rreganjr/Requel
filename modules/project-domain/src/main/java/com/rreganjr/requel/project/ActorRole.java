/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project;

/**
 * #468: who does a step, apart from its wording. A step (or a scenario's name) with a role stores
 * only the action ("ends the room"); the actor is filled in where the step is shown. A step with no
 * role names its actor in its wording, as every step did before #468.
 */
public enum ActorRole {
	/** The primary actor of the use case the step is shown in. */
	PRIMARY_ACTOR,
	/** The system the project describes. */
	SYSTEM,
	/** One actor of the project, named on the step. */
	NAMED;

	/**
	 * Parses an API or XML value: one of the constants (any case), or null/blank for none.
	 *
	 * @throws IllegalArgumentException for any other value
	 */
	public static ActorRole parse(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		for (ActorRole role : values()) {
			if (role.name().equalsIgnoreCase(value.trim())) {
				return role;
			}
		}
		throw new IllegalArgumentException("Unknown actor role \"" + value
				+ "\"; expected PRIMARY_ACTOR, SYSTEM or NAMED.");
	}
}
