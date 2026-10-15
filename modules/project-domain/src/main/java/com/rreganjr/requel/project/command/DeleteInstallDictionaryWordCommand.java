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

import com.rreganjr.platform.command.EditCommand;

/**
 * Removes a word from the installation-wide dictionary (issue #319). Requires the system
 * administrator role, and is denied on the MCP gateway. The WordNet corpus and the jazzy word
 * lists are not editable this way.
 *
 * @author ron
 */
public interface DeleteInstallDictionaryWordCommand extends EditCommand {

	/**
	 * @param wordId -
	 *            the id of the installation dictionary word to remove.
	 */
	public void setWordId(Long wordId);
}
