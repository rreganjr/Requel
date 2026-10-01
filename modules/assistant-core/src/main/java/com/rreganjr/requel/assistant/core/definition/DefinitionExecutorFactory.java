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

import com.rreganjr.requel.assistant.api.RequelAssistant;

/**
 * Issue #260: builds the assistant that runs a definition. Implemented by the AI module and
 * present only when AI is enabled; with no factory, definitions are not run.
 */
public interface DefinitionExecutorFactory {

	/** The executor for {@code definition}. It also implements {@link DefinitionBacked}. */
	RequelAssistant<?> executorFor(AssistantDefinition definition);
}
