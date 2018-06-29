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
package com.rreganjr.requel.project.impl.command;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryContainer;
import com.rreganjr.requel.project.command.DeleteStoryCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RemoveActorFromActorContainerCommand;
import com.rreganjr.requel.project.command.RemoveGoalFromGoalContainerCommand;
import com.rreganjr.requel.project.command.RemoveStoryFromStoryContainerCommand;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * Delete a story from a project, cleaning up references from other project
 * entities, story relations and annotations.
 * 
 * @author ron
 */
@Controller("deleteStoryCommand")
@Scope("prototype")
public class DeleteStoryCommandImpl extends AbstractEditProjectCommand implements
		DeleteStoryCommand {

	private Story story;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public DeleteStoryCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setStory(Story story) {
		this.story = story;
	}

	protected Story getStory() {
		return story;
	}

	@Override
	public void execute() throws Exception {
		Story story = getRepository().get(getStory());
		User editedBy = getRepository().get(getEditedBy());
		Set<Annotation> annotations = new HashSet<Annotation>(story.getAnnotations());
		for (Annotation annotation : annotations) {
			RemoveAnnotationFromAnnotatableCommand removeAnnotationFromAnnotatableCommand = getAnnotationCommandFactory()
					.newRemoveAnnotationFromAnnotatableCommand();
			removeAnnotationFromAnnotatableCommand.setEditedBy(editedBy);
			removeAnnotationFromAnnotatableCommand.setAnnotatable(story);
			removeAnnotationFromAnnotatableCommand.setAnnotation(annotation);
			getCommandHandler().execute(removeAnnotationFromAnnotatableCommand);
		}
		// remove this entity as a referrer to any terms
		for (GlossaryTerm term : story.getProjectOrDomain().getGlossaryTerms()) {
			if (term.getReferrers().contains(story)) {
				term.getReferrers().remove(story);
			}
		}
		for (Actor actor : story.getActors()) {
			RemoveActorFromActorContainerCommand removeActorFromActorContainerCommand = getProjectCommandFactory().newRemoveActorFromActorContainerCommand();
			removeActorFromActorContainerCommand.setEditedBy(editedBy);
			removeActorFromActorContainerCommand.setActor(actor);
			removeActorFromActorContainerCommand.setActorContainer(story);
			getCommandHandler().execute(removeActorFromActorContainerCommand);
		}
		for (Goal goal : story.getGoals()) {
			RemoveGoalFromGoalContainerCommand removeGoalFromGoalContainerCommand = getProjectCommandFactory().newRemoveGoalFromGoalContainerCommand();
			removeGoalFromGoalContainerCommand.setEditedBy(editedBy);
			removeGoalFromGoalContainerCommand.setGoal(goal);
			removeGoalFromGoalContainerCommand.setGoalContainer(story);
			getCommandHandler().execute(removeGoalFromGoalContainerCommand);
		}
		Set<StoryContainer> storyReferers = new HashSet<StoryContainer>(story.getReferrers());
		for (StoryContainer storyContainer : storyReferers) {
			RemoveStoryFromStoryContainerCommand removeStoryFromStoryContainerCommand = getProjectCommandFactory().newRemoveStoryFromStoryContainerCommand();
			removeStoryFromStoryContainerCommand.setEditedBy(editedBy);
			removeStoryFromStoryContainerCommand.setStory(story);
			removeStoryFromStoryContainerCommand.setStoryContainer(storyContainer);
			getCommandHandler().execute(removeStoryFromStoryContainerCommand);
		}
		story.getProjectOrDomain().getStories().remove(story);
		getRepository().delete(story);
	}

}
