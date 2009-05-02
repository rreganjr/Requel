/*
 * $Id: AddStoryToStoryContainerCommandImpl.java,v 1.6 2009/03/30 11:54:29 rregan Exp $
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
package edu.harvard.fas.rregan.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.StoryContainer;
import edu.harvard.fas.rregan.requel.project.command.AddStoryToStoryContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("addStoryToStoryContainerCommand")
@Scope("prototype")
public class AddStoryToStoryContainerCommandImpl extends AbstractEditProjectCommand implements
		AddStoryToStoryContainerCommand {

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 */
	@Autowired
	public AddStoryToStoryContainerCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	private Story story;
	private StoryContainer storyContainer;

	@Override
	public Story getStory() {
		return story;
	}

	@Override
	public void setStory(Story story) {
		this.story = story;
	}

	@Override
	public StoryContainer getStoryContainer() {
		return storyContainer;
	}

	@Override
	public void setStoryContainer(StoryContainer storyContainer) {
		this.storyContainer = storyContainer;
	}

	@Override
	public void execute() {
		Story addedStory = getRepository().get(getStory());
		StoryContainer addingContainer = getRepository().get(getStoryContainer());
		addedStory.getReferers().add(addingContainer);
		addingContainer.getStories().add(addedStory);

		// replaced the supplied objects with the updated objects for retrieval.
		addedStory = getRepository().merge(addedStory);
		addingContainer = getRepository().merge(addingContainer);
		setStory(addedStory);
		setStoryContainer(addingContainer);
	}
}
