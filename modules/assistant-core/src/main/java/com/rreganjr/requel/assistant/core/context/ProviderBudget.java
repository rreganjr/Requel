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
package com.rreganjr.requel.assistant.core.context;

/**
 * Issue #261: the characters one provider may add. Providers offer entities in priority order and
 * stop at the first that does not fit, so what is cut is always the tail.
 */
public final class ProviderBudget {

	private final int cap;
	private int used;

	public ProviderBudget(int cap) {
		this.cap = Math.max(0, cap);
	}

	/** Count {@code entity} and return true if it fits; false (and nothing counted) if not. */
	public boolean tryAdd(RelatedEntity entity) {
		int size = entity.size();
		if (used + size > cap) {
			return false;
		}
		used += size;
		return true;
	}

	public int used() {
		return used;
	}

	public int cap() {
		return cap;
	}
}
