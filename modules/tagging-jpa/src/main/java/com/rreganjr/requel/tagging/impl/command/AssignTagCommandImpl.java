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
import com.rreganjr.requel.tagging.Taggable;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.command.AssignTagCommand;
import com.rreganjr.requel.tagging.command.TagCommandFactory;
import com.rreganjr.requel.tagging.impl.TagImpl;

/**
 * Assign a tag to a taggable entity (idempotent — the assignment set ignores duplicates).
 *
 * @author ron
 */
@Controller("assignTagCommand")
@Scope("prototype")
public class AssignTagCommandImpl extends AbstractTagCommand implements AssignTagCommand {

	private Taggable taggable;

	@Autowired
	public AssignTagCommandImpl(CommandHandler commandHandler, TagCommandFactory tagCommandFactory,
			TagRepository repository) {
		super(commandHandler, tagCommandFactory, repository);
	}

	@Override
	public void setTaggable(Taggable taggable) {
		this.taggable = taggable;
	}

	@Override
	protected String stakeholderPermissionType() {
		return "Edit";
	}

	@Override
	public void execute() {
		TagImpl tagImpl = (TagImpl) getRepository().get(getTag());
		Taggable managedTaggable = getRepository().get(taggable);
		tagImpl.getTaggables().add(managedTaggable);
		getRepository().merge(tagImpl);
		setTag(tagImpl);
	}
}
