/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryContainer;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ron
 */
@Controller("editStoryCommand")
@Scope("prototype")
public class EditStoryCommandImpl extends AbstractEditProjectOrDomainEntityCommand implements
		EditStoryCommand, AuthorizableCommand, ProjectScopedCommand {

	private static final Logger log = LoggerFactory.getLogger(EditStoryCommandImpl.class);

	private StoryContainer storyContainer;
	private Story story;
	private String text;
	private String storyTypeName;
	private String primaryActorName;

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
	public void setPrimaryActorName(String primaryActorName) {
		this.primaryActorName = primaryActorName;
	}

	@Override
	public String getPrimaryActorName() {
		return primaryActorName;
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

		// Resolve the primary actor before any persistence calls
		Actor resolvedPrimaryActor = null;
		if (getPrimaryActorName() != null && !getPrimaryActorName().trim().isEmpty()) {
			try {
				resolvedPrimaryActor = getProjectRepository().findActorByProjectOrDomainAndName(
						projectOrDomain, getPrimaryActorName());
			} catch (NoSuchEntityException e) {
				log.warn("Primary actor '{}' not found for story '{}' — leaving unchanged",
						getPrimaryActorName(), getName());
			}
		}

		if (storyImpl == null) {
			storyImpl = getProjectRepository().persist(
					new StoryImpl(projectOrDomain, editedBy, getName(), getText(), getStoryType()));
			storyImpl.setPrimaryActor(resolvedPrimaryActor);
		} else {
			storyImpl.setName(getName());
			storyImpl.setText(getText());
			storyImpl.setStoryType(getStoryType());
			// Mirror UseCase pattern: merge first so the entity is managed, then set actor
			storyImpl = getProjectRepository().merge(storyImpl);
			Actor priorActor = storyImpl.getPrimaryActor();
			if (priorActor != null && !priorActor.equals(resolvedPrimaryActor)) {
				// Use direct SQL to avoid loading the @ManyToAny collection, which would throw
				// NoSuchEntityException if the collection contains stale rows for deleted entities.
				getProjectRepository().removeActorContainerFromActorJoinTable(
						priorActor.getId(), storyImpl.getId());
			}
			storyImpl.setPrimaryActor(resolvedPrimaryActor);
		}

		if (storyContainer != null) {
			storyImpl.getReferers().add(storyContainer);
			storyContainer.getStories().add(storyImpl);
		}
		// Add to actor's referers using direct SQL (INSERT IGNORE is idempotent; avoids loading
		// the @ManyToAny collection which may contain stale rows for previously deleted entities).
		if (resolvedPrimaryActor != null) {
			getProjectRepository().addActorContainerToActorJoinTable(
					resolvedPrimaryActor.getId(), storyImpl.getId(),
					"com.rreganjr.requel.project.Story");
		}
		setStory(getProjectRepository().merge(storyImpl));
	}

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			getAssistantManager().analyzeStory(getStory());
		}
	}

	@Override
	public Project getProject() {
		if (storyContainer instanceof Project project) return project;
		if (story != null && story.getProjectOrDomain() instanceof Project project) return project;
		return null;
	}

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(Story.class, "Edit");
	}
}
