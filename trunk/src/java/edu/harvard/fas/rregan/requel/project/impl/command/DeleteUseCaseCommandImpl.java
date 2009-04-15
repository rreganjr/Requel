/*
 * $Id: DeleteUseCaseCommandImpl.java,v 1.5 2009/03/30 11:54:27 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.command;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.UseCase;
import edu.harvard.fas.rregan.requel.project.command.DeleteUseCaseCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.command.RemoveActorFromActorContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.RemoveGoalFromGoalContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.RemoveStoryFromStoryContainerCommand;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

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
