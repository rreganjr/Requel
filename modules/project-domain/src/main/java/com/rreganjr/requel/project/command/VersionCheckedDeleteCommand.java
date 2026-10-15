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
package com.rreganjr.requel.project.command;

import com.rreganjr.platform.command.EditCommand;

/**
 * A delete that honours the caller's optimistic-lock version (issue #296). Before it, the seven
 * entity deletes took a {@code version} in their input and dropped it, so a caller who read an
 * entity, lost a race with another editor, and then deleted it removed the other editor's work
 * without being told.
 *
 * @author ron
 */
public interface VersionCheckedDeleteCommand extends EditCommand {

	/**
	 * The version of the entity the caller last read. When non-null and different from the
	 * persisted version, the delete is refused as stale; {@code null} skips the check, which is
	 * what a command deleting children on another command's behalf passes.
	 */
	public void setExpectedVersion(Integer expectedVersion);
}
