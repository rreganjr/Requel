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
 * #468: create or edit a position that gives the step (or scenario) its issue is on an actor role,
 * keeping only the action as its name. Assistant internal: the step-structure check offers it.
 */
public interface EditSetStepActorPositionCommand extends EditPositionCommand {

	/** The name the step takes: the action without its actor ("ends the room"). */
	public void setProposedName(String proposedName);

	/** The step's name the proposal was built from. */
	public void setExpectedName(String expectedName);

	/** PRIMARY_ACTOR, SYSTEM or NAMED. */
	public void setActorRoleName(String actorRoleName);

	/** The actor a NAMED role names. */
	public void setActorName(String actorName);
}
