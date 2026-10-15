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

import com.rreganjr.requel.project.InvalidDefinitionException;

/**
 * Issue #260: an assistant definition that cannot be saved or seeded, with every reason. #264: each
 * reason names its field ({@link #fieldProblems()}).
 */
public class InvalidAssistantDefinitionException extends InvalidDefinitionException {
	private static final long serialVersionUID = 1L;

	public InvalidAssistantDefinitionException(String key, List<Problem> problems) {
		super(key, problems);
	}
}
