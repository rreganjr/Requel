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

import java.util.Set;

/**
 * Create or update a {@link com.rreganjr.requel.tagging.TagCategory}. When {@code categoryId} is
 * null the command creates (or reuses by normalized {@code (scope, name)}) a category; otherwise it
 * updates that category. The name is normalized to a slug on write.
 *
 * @author ron
 */
public interface EditTagCategoryCommand extends TagCategoryCommand {

	void setCategoryId(Long categoryId);

	void setName(String name);

	void setExclusive(boolean exclusive);

	void setColor(String color);

	void setAllowedEntityTypes(Set<String> allowedEntityTypes);

	void setValues(Set<String> values);
}
