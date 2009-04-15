/*
 * $Id: AddStoryToStoryContainerController.java,v 1.3 2008/12/13 00:41:05 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.project;

import nextapp.echo2.app.event.ActionEvent;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.StoryContainer;
import edu.harvard.fas.rregan.requel.project.command.AddStoryToStoryContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelCommandController;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.navigation.event.SelectEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;

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
					// TODO: this may not be needed because the update listeners for things that
					// are story containers will probably pickup the previous event and get the
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