/*
 * $Id: EditStoryCommandImpl.java,v 1.11 2009/03/30 11:54:31 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.StoryContainer;
import edu.harvard.fas.rregan.requel.project.StoryType;
import edu.harvard.fas.rregan.requel.project.command.EditStoryCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.StoryImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("editStoryCommand")
@Scope("prototype")
public class EditStoryCommandImpl extends AbstractEditProjectOrDomainEntityCommand implements
		EditStoryCommand {

	private StoryContainer storyContainer;
	private Story story;
	private String text;
	private String storyTypeName;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public EditStoryCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	public Story getStory() {
		return story;
	}

	public void setStory(Story story) {
		this.story = story;
	}

	@Override
	public void setStoryContainer(StoryContainer storyContainer) {
		this.storyContainer = storyContainer;
	}

	protected StoryContainer getStoryContainer() {
		return storyContainer;
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	protected String getText() {
		return text;
	}

	protected StoryType getStoryType() {
		if (storyTypeName != null) {
			return StoryType.valueOf(storyTypeName);
		}
		return null;
	}

	@Override
	public void setStoryTypeName(String storyTypeName) {
		this.storyTypeName = storyTypeName;
	}

	@Override
	public void execute() {
		StoryContainer storyContainer = getProjectRepository().get(getStoryContainer());
		User editedBy = getProjectRepository().get(getEditedBy());
		StoryImpl storyImpl = (StoryImpl) getStory();
		ProjectOrDomain projectOrDomain = null;
		if (storyImpl != null) {
			projectOrDomain = storyImpl.getProjectOrDomain();
		} else if (storyContainer != null) {
			if (storyContainer instanceof ProjectOrDomain) {
				projectOrDomain = (ProjectOrDomain) storyContainer;
			} else if (storyContainer instanceof ProjectOrDomainEntity) {
				projectOrDomain = ((ProjectOrDomainEntity) storyContainer).getProjectOrDomain();
			} else {
				// TODO: error?
			}
		}
		projectOrDomain = getRepository().get(projectOrDomain);

		// check for uniqueness
		try {
			Story existing = getProjectRepository().findStoryByProjectOrDomainAndName(
					projectOrDomain, getName());
			if (storyImpl == null) {
				throw EntityException.uniquenessConflict(Story.class, existing, FIELD_NAME,
						EntityExceptionActionType.Creating);
			} else if (!existing.equals(storyImpl)) {
				throw EntityException.uniquenessConflict(Story.class, existing, FIELD_NAME,
						EntityExceptionActionType.Updating);
			}
		} catch (NoSuchEntityException e) {
		}

		if (storyImpl == null) {
			storyImpl = getProjectRepository().persist(
					new StoryImpl(projectOrDomain, editedBy, getName(), getText(), getStoryType()));
		} else {
			storyImpl.setName(getName());
			storyImpl.setText(getText());
			storyImpl.setStoryType(getStoryType());
		}
		if (storyContainer != null) {
			storyImpl.getReferers().add(storyContainer);
			storyContainer.getStories().add(storyImpl);
		}
		setStory(getProjectRepository().merge(storyImpl));
	}

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			getAssistantManager().analyzeStory(getStory());
		}
	}
}
