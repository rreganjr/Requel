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
package com.rreganjr.requel.tagging.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.tagging.TagCategory;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.command.DeleteTagCategoryCommand;
import com.rreganjr.requel.tagging.command.TagCommandFactory;

/**
 * Delete a tag category (its allowed-type and value child rows cascade). Tags keep their category
 * string and simply lose the category's rules.
 *
 * @author ron
 */
@Controller("deleteTagCategoryCommand")
@Scope("prototype")
public class DeleteTagCategoryCommandImpl extends AbstractTagCategoryCommand implements DeleteTagCategoryCommand {

	private Long categoryId;

	@Autowired
	public DeleteTagCategoryCommandImpl(CommandHandler commandHandler, TagCommandFactory tagCommandFactory,
			TagRepository repository) {
		super(commandHandler, tagCommandFactory, repository);
	}

	@Override
	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	@Override
	protected String stakeholderPermissionType() {
		return "Delete";
	}

	@Override
	public void execute() {
		TagCategory category = getTagRepository().findCategoryById(categoryId);
		if (category == null) {
			return;
		}
		getRepository().delete(getRepository().get(category));
	}
}
