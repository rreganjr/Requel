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

import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.tagging.Tag;

/**
 * Common contract for tag mutation commands. The <em>project scope</em> is passed as an
 * opaque {@code Object} (resolved and injected by the API layer) so this domain module
 * stays free of any dependency on the project module — mirroring how
 * {@code EditAnnotationCommand.setGroupingObject(Object)} carries the project through the
 * annotation commands. A {@code null} scope means a global (system) tag.
 *
 * @author ron
 */
public interface TagCommand extends EditCommand {

	/**
	 * Set the tag to operate on. For {@link EditTagCommand} this is optional (null =
	 * create); for the others it identifies the target tag.
	 */
	void setTag(Tag tag);

	/**
	 * @return the managed tag after {@code execute()} (for result extraction).
	 */
	Tag getTag();

	/**
	 * Set the owning project as an opaque scope for authorization/scoping. {@code null}
	 * denotes a global tag. The implementation resolves the concrete project type.
	 */
	void setProjectScope(Object project);
}
