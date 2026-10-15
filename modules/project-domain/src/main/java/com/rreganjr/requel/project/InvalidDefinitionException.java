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
package com.rreganjr.requel.project;

import java.util.ArrayList;
import java.util.List;

/**
 * Issue #264: an assistant definition write broke one or more rules. Each problem names the field
 * it is about, so the command path can report it against the right form control.
 */
public class InvalidDefinitionException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;

	/** A broken rule: the definition field it concerns (null for the whole definition). */
	public record Problem(String field, String message) {
	}

	private final List<Problem> fieldProblems;

	public InvalidDefinitionException(String key, List<Problem> problems) {
		super("Assistant definition '" + key + "' is invalid: " + String.join("; ",
				messages(problems)));
		this.fieldProblems = List.copyOf(problems);
	}

	/** Every problem, with its field. */
	public List<Problem> fieldProblems() {
		return fieldProblems;
	}

	/** Every problem's message. */
	public List<String> problems() {
		return messages(fieldProblems);
	}

	private static List<String> messages(List<Problem> problems) {
		List<String> messages = new ArrayList<String>(problems.size());
		for (Problem problem : problems) {
			messages.add(problem.message());
		}
		return messages;
	}
}
