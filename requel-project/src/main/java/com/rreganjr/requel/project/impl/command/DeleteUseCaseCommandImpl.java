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
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.DeleteUseCaseCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RemoveActorFromActorContainerCommand;
import com.rreganjr.requel.project.command.RemoveGoalFromGoalContainerCommand;
import com.rreganjr.requel.project.command.RemoveStoryFromStoryContainerCommand;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * Delete a usecase from a project, cleaning up references from other project
 * entities, usecase relations and annotations.
 * 
 * @author ron
 */
@Controller("deleteUseCaseCommand")
@Scope("prototype")
public class DeleteUseCaseCommandImpl extends AbstractEditProjectCommand implements
		DeleteUseCaseCommand {

	private UseCase usecase;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public DeleteUseCaseCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setUseCase(UseCase usecase) {
		this.usecase = usecase;
	}

	protected UseCase getUseCase() {
		return usecase;
	}

	@Override
	public void execute() throws Exception {
		UseCase usecase = getRepository().get(getUseCase());
		User editedBy = getRepository().get(getEditedBy());
		Set<Annotation> annotations = new HashSet<Annotation>(usecase.getAnnotations());
		for (Annotation annotation : annotations) {
			RemoveAnnotationFromAnnotatableCommand removeAnnotationFromAnnotatableCommand = getAnnotationCommandFactory()
					.newRemoveAnnotationFromAnnotatableCommand();
			removeAnnotationFromAnnotatableCommand.setEditedBy(editedBy);
			removeAnnotationFromAnnotatableCommand.setAnnotatable(usecase);
			removeAnnotationFromAnnotatableCommand.setAnnotation(annotation);
			getCommandHandler().execute(removeAnnotationFromAnnotatableCommand);
		}
		// remove this entity as a referer to any terms
		for (GlossaryTerm term : usecase.getProjectOrDomain().getGlossaryTerms()) {
			if (term.getReferers().contains(usecase)) {
				term.getReferers().remove(usecase);
			}
		}
		Set<Actor> actors = new HashSet<Actor>(usecase.getActors());
		actors.add(usecase.getPrimaryActor());
		for (Actor actor : actors) {
			RemoveActorFromActorContainerCommand removeActorFromActorContainerCommand = getProjectCommandFactory()
					.newRemoveActorFromActorContainerCommand();
			removeActorFromActorContainerCommand.setActor(actor);
			removeActorFromActorContainerCommand.setActorContainer(usecase);
			removeActorFromActorContainerCommand.setEditedBy(getEditedBy());
			getCommandHandler().execute(removeActorFromActorContainerCommand);
		}
		Set<Goal> goals = new HashSet<Goal>(usecase.getGoals());
		for (Goal goal : goals) {
			RemoveGoalFromGoalContainerCommand removeGoalFromGoalContainerCommand = getProjectCommandFactory()
					.newRemoveGoalFromGoalContainerCommand();
			removeGoalFromGoalContainerCommand.setGoal(goal);
			removeGoalFromGoalContainerCommand.setGoalContainer(usecase);
			removeGoalFromGoalContainerCommand.setEditedBy(getEditedBy());
			getCommandHandler().execute(removeGoalFromGoalContainerCommand);
		}
		Set<Story> stories = new HashSet<Story>(usecase.getStories());
		for (Story story : stories) {
			RemoveStoryFromStoryContainerCommand removeStoryFromStoryContainerCommand = getProjectCommandFactory()
					.newRemoveStoryFromStoryContainerCommand();
			removeStoryFromStoryContainerCommand.setStory(story);
			removeStoryFromStoryContainerCommand.setStoryContainer(usecase);
			removeStoryFromStoryContainerCommand.setEditedBy(getEditedBy());
			getCommandHandler().execute(removeStoryFromStoryContainerCommand);
		}
		for (Scenario scenario : getProjectRepository().findScenariosUsedByUseCase(usecase)) {
			// TODO: add command RemoveUsecaseFromScenario
			scenario.getUsingUseCases().remove(usecase);
		}
		// TODO: delete the main scenario?
		usecase.getProjectOrDomain().getUseCases().remove(usecase);
		getRepository().delete(usecase);
	}

}
