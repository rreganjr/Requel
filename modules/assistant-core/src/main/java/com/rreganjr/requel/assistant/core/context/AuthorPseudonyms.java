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
package com.rreganjr.requel.assistant.core.context;

import java.util.LinkedHashMap;
import java.util.Map;

import com.rreganjr.platform.identity.User;

/**
 * Issue #262: replaces usernames in one context pack with roles, so no username reaches a model.
 * Any assistant identity (#260) is {@code assistant}; everyone else is {@code user-1},
 * {@code user-2}, …
 * numbered by first appearance, so the same author keeps the same label within the pack.
 */
final class AuthorPseudonyms {

	private final Map<String, String> labels = new LinkedHashMap<>();

	String of(User user) {
		if (user == null || user.getUsername() == null) {
			return null;
		}
		String username = user.getUsername();
		// #260: every assistant identity reads as the one role, as before there were several.
		if (User.isAssistant(user)) {
			return User.ASSISTANT_USERNAME;
		}
		return labels.computeIfAbsent(username, u -> "user-" + (labels.size() + 1));
	}
}
