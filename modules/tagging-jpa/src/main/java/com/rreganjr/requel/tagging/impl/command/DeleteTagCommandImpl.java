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
import com.rreganjr.requel.tagging.Tag;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.command.DeleteTagCommand;
import com.rreganjr.requel.tagging.command.TagCommandFactory;
import com.rreganjr.requel.tagging.impl.TagImpl;

/**
 * Delete a tag and all of its assignments (the {@code tag_taggable} rows are removed with
 * the tag; the tagged entities themselves are untouched).
 *
 * @author ron
 */
@Controller("deleteTagCommand")
@Scope("prototype")
public class DeleteTagCommandImpl extends AbstractTagCommand implements DeleteTagCommand {

	@Autowired
	public DeleteTagCommandImpl(CommandHandler commandHandler, TagCommandFactory tagCommandFactory,
			TagRepository repository) {
		super(commandHandler, tagCommandFactory, repository);
	}

	@Override
	protected String stakeholderPermissionType() {
		return "Delete";
	}

	@Override
	public void execute() {
		TagImpl tagImpl = (TagImpl) getRepository().get(getTag());
		if (tagImpl == null) {
			return;
		}
		// Detach from all assigned entities, then delete the definition.
		tagImpl.getTaggables().clear();
		getRepository().delete(tagImpl);
	}
}
