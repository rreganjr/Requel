/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
package com.rreganjr.requel.ui.project;

import nextapp.echo2.app.event.ActionEvent;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.project.StoryContainer;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RemoveStoryFromStoryContainerCommand;
import com.rreganjr.requel.ui.AbstractRequelCommandController;
import net.sf.echopm.navigation.event.EventDispatcher;
import net.sf.echopm.navigation.event.UpdateEntityEvent;

/**
 * This controller is to be used in a Story container entity editor where the
 * editor contains a StoriesTable and allows removing Storys from the entity.
 * 
 * @author ron
 */
public class RemoveStoryFromStoryContainerController extends AbstractRequelCommandController {
	static final long serialVersionUID = 0;

	private final StoryContainer storyContainer;

	/**
	 * @param eventDispatcher
	 * @param commandFactory
	 * @param commandHandler
	 * @param storyContainer
	 */
	public RemoveStoryFromStoryContainerController(EventDispatcher eventDispatcher,
			ProjectCommandFactory commandFactory, CommandHandler commandHandler,
			StoryContainer storyContainer) {
		super(eventDispatcher, commandFactory, commandHandler);
		this.storyContainer = storyContainer;
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event instanceof RemoveStoryFromStoryContainerEvent) {
			RemoveStoryFromStoryContainerEvent removeEvent = (RemoveStoryFromStoryContainerEvent) event;
			if (storyContainer.equals(removeEvent.getStoryContainer())) {
				try {
					ProjectCommandFactory factory = getCommandFactory();
					RemoveStoryFromStoryContainerCommand command = factory
							.newRemoveStoryFromStoryContainerCommand();
					command.setEditedBy(null); // TODO: need user?
					command.setStory(removeEvent.getStory());
					command.setStoryContainer(storyContainer);
					command = getCommandHandler().execute(command);
					fireEvent(new UpdateEntityEvent(this, null, command.getStory()));
					fireEvent(new UpdateEntityEvent(this, null, storyContainer));
				} catch (Exception e) {
					// TODO: what can fail?
					log.error(e, e);
				}
			}
		}
	}
}
