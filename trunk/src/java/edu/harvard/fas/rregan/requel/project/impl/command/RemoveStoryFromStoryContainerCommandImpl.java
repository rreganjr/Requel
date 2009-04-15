/*
 * $Id: RemoveStoryFromStoryContainerCommandImpl.java,v 1.5 2009/03/30 11:54:31 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.StoryContainer;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.command.RemoveStoryFromStoryContainerCommand;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("removeStoryFromStoryContainerCommand")
@Scope("prototype")
public class RemoveStoryFromStoryContainerCommandImpl extends AbstractEditProjectCommand implements
		RemoveStoryFromStoryContainerCommand {

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 */
	@Autowired
	public RemoveStoryFromStoryContainerCommandImpl(AssistantFacade assistantManager,
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
	public StoryContainer getStoryContainer() {
		return storyContainer;
	}

	@Override
	public void setStory(Story story) {
		this.story = story;
	}

	@Override
	public void setStoryContainer(StoryContainer storyContainer) {
		this.storyContainer = storyContainer;
	}

	@Override
	public void execute() {
		Story removedStory = getProjectRepository().get(getStory());
		StoryContainer removingContainer = getProjectRepository().get(getStoryContainer());
		removedStory.getReferers().remove(removingContainer);
		removingContainer.getStories().remove(removedStory);

		// replaced the supplied objects with the updated objects for retrieval.
		removedStory = getRepository().merge(removedStory);
		removingContainer = getRepository().merge(removingContainer);
		setStory(removedStory);
		setStoryContainer(removingContainer);
	}
}
