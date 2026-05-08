/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025, 2026 Ron Regan Jr. All Rights Reserved.
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
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryContainer;
import com.rreganjr.requel.project.command.AddStoryToStoryContainerCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.UseCaseImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.project.impl.repository.jpa.JpaProjectRepository;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("addStoryToStoryContainerCommand")
@Scope("prototype")
public class AddStoryToStoryContainerCommandImpl extends AbstractEditProjectCommand implements
		AddStoryToStoryContainerCommand, ProjectScopedCommand {

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
		Story addedStory = getProjectRepository().get(getStory());
		StoryContainer addingContainer = getProjectRepository().get(getStoryContainer());

		// Hibernate 6.5 bug: @ManyToAny collection insertion generates invalid SQL for the
		// story_storycontainers join table. Use a native INSERT instead.
		JpaProjectRepository jpaRepo = (JpaProjectRepository) getProjectRepository();
		jakarta.persistence.PersistenceUnitUtil puu = jpaRepo.getEntityManager()
				.getEntityManagerFactory().getPersistenceUnitUtil();
		Long storyId = (Long) puu.getIdentifier(addedStory);
		Long containerId = (Long) puu.getIdentifier(addingContainer);
		jpaRepo.addStoryContainerToStoryJoinTable(storyId, containerId, storyContainerDiscriminator(addingContainer));
		jpaRepo.getEntityManager().refresh(addedStory);

		addingContainer.getStories().add(addedStory);
		addingContainer = getRepository().merge(addingContainer);
		setStory(addedStory);
		setStoryContainer(addingContainer);
	}

	@Override
	public Project getProject() {
		// StoryContainer is Project, Actor, Goal, or UseCase. Project is direct;
		// the others are ProjectOrDomainEntity so getProjectOrDomain walks back
		// to the Project.
		if (storyContainer instanceof Project project) return project;
		if (storyContainer instanceof ProjectOrDomainEntity pode
				&& pode.getProjectOrDomain() instanceof Project project) return project;
		if (story != null && story.getProjectOrDomain() instanceof Project project) return project;
		return null;
	}

	private static String storyContainerDiscriminator(StoryContainer container) {
		if (container instanceof ProjectImpl)  return "com.rreganjr.requel.project.Project";
		if (container instanceof ActorImpl)    return "com.rreganjr.requel.project.Actor";
		if (container instanceof GoalImpl)     return "com.rreganjr.requel.project.Goal";
		if (container instanceof UseCaseImpl)  return "com.rreganjr.requel.project.UseCase";
		throw new IllegalStateException("Unknown StoryContainer type: " + container.getClass().getName());
	}
}
