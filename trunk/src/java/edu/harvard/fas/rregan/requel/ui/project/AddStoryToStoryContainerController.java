/*
 * $Id$
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
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.StoryContainer;
import edu.harvard.fas.rregan.requel.project.command.AddStoryToStoryContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelCommandController;
import net.sf.echopm.navigation.event.EventDispatcher;
import net.sf.echopm.navigation.event.SelectEntityEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;

/**
 * This controller is to be used in a story container entity editor where the
 * editor contains a StorysTable and allows adding existing storys to the
 * entity.
 * 
 * @author ron
 */
public class AddStoryToStoryContainerController extends AbstractRequelCommandController {
	static final long serialVersionUID = 0;

	private final StoryContainer storyContainer;

	/**
	 * @param eventDispatcher
	 * @param commandFactory
	 * @param commandHandler
	 * @param storyContainer
	 */
	public AddStoryToStoryContainerController(EventDispatcher eventDispatcher,
			ProjectCommandFactory commandFactory, CommandHandler commandHandler,
			StoryContainer storyContainer) {
		super(eventDispatcher, commandFactory, commandHandler);
		this.storyContainer = storyContainer;
	}

	/**
	 * FIXME: there is a flaw in that the select entity event doesn't have an
	 * expected destination so any select entity event with a story will trigger
	 * this controller and may produce unintended results.
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		if (event instanceof SelectEntityEvent) {
			SelectEntityEvent selectEntityEvent = (SelectEntityEvent) event;
			if (selectEntityEvent.getObject() instanceof Story) {
				Story story = (Story) selectEntityEvent.getObject();
				try {
					ProjectCommandFactory factory = getCommandFactory();
					AddStoryToStoryContainerCommand command = factory
							.newAddStoryToStoryContainerCommand();
					command.setEditedBy(null); // TODO: need user?
					command.setStory(story);
					command.setStoryContainer(storyContainer);
					command = getCommandHandler().execute(command);
					fireEvent(new UpdateEntityEvent(this, null, command.getStory()));
					// TODO: this may not be needed because the update listeners
					// for things that
					// are story containers will probably pickup the previous
					// event and get the
					// updated story container from the story.
					fireEvent(new UpdateEntityEvent(this, null, command.getStoryContainer()));
				} catch (Exception e) {
					// TODO: what can fail?
					log.error(e, e);
				}
			}
		}
	}
}
