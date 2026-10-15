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

import com.rreganjr.command.AbstractCommandFactory;
import com.rreganjr.command.CommandFactoryStrategy;
import com.rreganjr.requel.tagging.command.AssignTagCommand;
import com.rreganjr.requel.tagging.command.DeleteTagCategoryCommand;
import com.rreganjr.requel.tagging.command.DeleteTagCommand;
import com.rreganjr.requel.tagging.command.EditTagCategoryCommand;
import com.rreganjr.requel.tagging.command.EditTagCommand;
import com.rreganjr.requel.tagging.command.TagCommandFactory;
import com.rreganjr.requel.tagging.command.UnassignTagCommand;

/**
 * @author ron
 */
@Controller("tagCommandFactory")
@Scope("singleton")
public class TagCommandFactoryImpl extends AbstractCommandFactory implements TagCommandFactory {

	@Autowired
	public TagCommandFactoryImpl(CommandFactoryStrategy creationStrategy) {
		super(creationStrategy);
	}

	@Override
	public EditTagCommand newEditTagCommand() {
		return (EditTagCommand) getCreationStrategy().newInstance(EditTagCommandImpl.class);
	}

	@Override
	public DeleteTagCommand newDeleteTagCommand() {
		return (DeleteTagCommand) getCreationStrategy().newInstance(DeleteTagCommandImpl.class);
	}

	@Override
	public AssignTagCommand newAssignTagCommand() {
		return (AssignTagCommand) getCreationStrategy().newInstance(AssignTagCommandImpl.class);
	}

	@Override
	public UnassignTagCommand newUnassignTagCommand() {
		return (UnassignTagCommand) getCreationStrategy().newInstance(UnassignTagCommandImpl.class);
	}

	@Override
	public EditTagCategoryCommand newEditTagCategoryCommand() {
		return (EditTagCategoryCommand) getCreationStrategy().newInstance(EditTagCategoryCommandImpl.class);
	}

	@Override
	public DeleteTagCategoryCommand newDeleteTagCategoryCommand() {
		return (DeleteTagCategoryCommand) getCreationStrategy().newInstance(DeleteTagCategoryCommandImpl.class);
	}
}
