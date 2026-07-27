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
package com.rreganjr.requel.tagging.command;

import com.rreganjr.requel.tagging.Tag;

/**
 * Create a new tag or edit an existing one. When {@link #setTag(Tag)} is null the command
 * creates a tag (or reuses an existing one with the same normalized identity in scope);
 * otherwise it updates the supplied tag.
 *
 * @author ron
 */
public interface EditTagCommand extends TagCommand {

	void setCategory(String category);

	void setValue(String value);

	void setDescription(String description);

	void setColor(String color);

	/**
	 * @return the created or updated tag after {@code execute()}.
	 */
	Tag getTag();
}
