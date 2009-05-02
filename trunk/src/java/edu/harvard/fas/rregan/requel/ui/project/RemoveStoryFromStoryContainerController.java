/*
 * $Id: RemoveStoryFromStoryContainerController.java,v 1.2 2008/12/13 00:41:08 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.requel.ui.project;

import nextapp.echo2.app.event.ActionEvent;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.StoryContainer;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.command.RemoveStoryFromStoryContainerCommand;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelCommandController;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;

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
