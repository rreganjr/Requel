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
package com.rreganjr.requel.project.command;

import com.rreganjr.requel.annotation.command.EditPositionCommand;

/**
 * #468: create or edit a position that adds an actor to a use case, offered when a step names an
 * actor that isn't in one of its use cases. Assistant internal.
 */
public interface EditAddActorToUseCasePositionCommand extends EditPositionCommand {

	/** The actor to add. */
	public void setActorName(String actorName);

	/** The use case to add it to. */
	public void setUseCaseName(String useCaseName);
}
