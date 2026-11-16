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
 * Issue #472: create or edit a position that merges two steps of a scenario into one. Assistant
 * internal: the step-sequence check offers it on its issue.
 */
public interface EditMergeStepsPositionCommand extends EditPositionCommand {

	/** The merged name the first step takes. */
	public void setProposedName(String proposedName);

	/** The first step's name the merge was built from. */
	public void setExpectedName(String expectedName);

	/** The second step's name the merge was built from; it must follow the first. */
	public void setExpectedSecondName(String expectedSecondName);
}
