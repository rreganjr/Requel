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

import java.util.List;
import java.util.Map;

/**
 * Issue #261: what to put in a pack beyond the base - the providers a definition names, in its
 * order, each one's character share (an id missing from {@code budgets} gets the default), and the
 * pack's character cap ({@code 0} = {@link ContextPackSizeLimits#getMaxTotalCharacters()}).
 */
public record PackSpec(List<String> providerIds, Map<String, Integer> budgets, int maxCharacters) {

	public PackSpec {
		providerIds = providerIds == null ? List.of() : List.copyOf(providerIds);
		budgets = budgets == null ? Map.of() : Map.copyOf(budgets);
	}

	/** The base pack only, as before #261. */
	public static PackSpec entityOnly() {
		return new PackSpec(List.of(ContextProviderRegistry.ENTITY), Map.of(), 0);
	}
}
