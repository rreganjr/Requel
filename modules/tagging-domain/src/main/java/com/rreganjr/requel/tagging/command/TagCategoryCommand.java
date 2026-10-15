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
import com.rreganjr.requel.tagging.TagCategory;

/**
 * Common contract for tag-category mutation commands. The project scope is passed as an opaque
 * {@code Object} (resolved by the API layer) so this domain module stays project-free, mirroring
 * {@link TagCommand}. A {@code null} scope denotes a global category.
 *
 * @author ron
 */
public interface TagCategoryCommand extends EditCommand {

	/** Owning project as an opaque scope; {@code null} = global. */
	void setProjectScope(Object project);

	/** @return the managed category after {@code execute()}. */
	TagCategory getTagCategory();
}
