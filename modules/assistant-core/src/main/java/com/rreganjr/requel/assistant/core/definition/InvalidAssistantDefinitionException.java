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
package com.rreganjr.requel.assistant.core.definition;

import java.util.List;

/** Issue #260: an assistant definition that cannot be saved or seeded, with every reason. */
public class InvalidAssistantDefinitionException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;

	private final List<String> problems;

	public InvalidAssistantDefinitionException(String key, List<String> problems) {
		super("Assistant definition '" + key + "' is invalid: " + String.join("; ", problems));
		this.problems = List.copyOf(problems);
	}

	public List<String> problems() {
		return problems;
	}
}
