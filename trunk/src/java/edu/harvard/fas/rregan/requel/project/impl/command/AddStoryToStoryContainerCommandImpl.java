/*
 * $Id: AddStoryToStoryContainerCommandImpl.java,v 1.6 2009/03/30 11:54:29 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
			ProjectCommandFactory projectCommandFactory, AnnotationCommandFactory annotationCommandFactory,
			CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory, annotationCommandFactory, commandHandler);
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
