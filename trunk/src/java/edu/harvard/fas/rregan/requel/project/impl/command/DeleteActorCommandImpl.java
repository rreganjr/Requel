/*
 * $Id: DeleteActorCommandImpl.java,v 1.8 2009/03/30 11:54:27 rregan Exp $
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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.NoSuchPositionException;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.DeletePositionCommand;
import edu.harvard.fas.rregan.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.ActorContainer;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.UseCase;
import edu.harvard.fas.rregan.requel.project.command.DeleteActorCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.command.RemoveActorFromActorContainerCommand;
import edu.harvard.fas.rregan.requel.project.impl.AddActorPosition;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * Delete a actor from a project, cleaning up references from other project
 * entities, actor relations and annotations.
 * 
 * @author ron
 */
@Controller("deleteActorCommand")
@Scope("prototype")
public class DeleteActorCommandImpl extends AbstractEditProjectCommand implements
		DeleteActorCommand {

	private Actor actor;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public DeleteActorCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setActor(Actor actor) {
		this.actor = actor;
	}

	protected Actor getActor() {
		return actor;
	}

	@Override
	public void execute() throws Exception {
		Actor actor = getRepository().get(getActor());
		User editedBy = getRepository().get(getEditedBy());
		Set<Annotation> annotations = new HashSet<Annotation>(actor.getAnnotations());
		for (Annotation annotation : annotations) {
			RemoveAnnotationFromAnnotatableCommand removeAnnotationFromAnnotatableCommand = getAnnotationCommandFactory()
					.newRemoveAnnotationFromAnnotatableCommand();
			removeAnnotationFromAnnotatableCommand.setEditedBy(editedBy);
			removeAnnotationFromAnnotatableCommand.setAnnotatable(actor);
			removeAnnotationFromAnnotatableCommand.setAnnotation(annotation);
			getCommandHandler().execute(removeAnnotationFromAnnotatableCommand);
		}
		// remove this entity as a referer to any terms
		for (GlossaryTerm term : actor.getProjectOrDomain().getGlossaryTerms()) {
			if (term.getReferers().contains(actor)) {
				term.getReferers().remove(actor);
			}
		}
		Set<ActorContainer> actorReferers = new HashSet<ActorContainer>(actor.getReferers());
		for (ActorContainer actorContainer : actorReferers) {
			RemoveActorFromActorContainerCommand removeActorFromActorContainerCommand = getProjectCommandFactory()
					.newRemoveActorFromActorContainerCommand();
			removeActorFromActorContainerCommand.setEditedBy(editedBy);
			removeActorFromActorContainerCommand.setActor(actor);
			removeActorFromActorContainerCommand.setActorContainer(actorContainer);
			getCommandHandler().execute(removeActorFromActorContainerCommand);
			if (actorContainer instanceof UseCase) {
				UseCase useCase = (UseCase) actorContainer;
				if (useCase.getPrimaryActor().equals(actor)) {
					throw new RuntimeException("The actor \"" + actor.getDescription()
							+ "\" is the primary actor for \"" + useCase.getDescription()
							+ "\" and cannot be deleted unless the use case is deleted first.");
				}
			}
		}
		try {
			AddActorPosition actorPosition = getProjectRepository().findAddActorPosition(
					actor.getProjectOrDomain(), actor.getName());
			DeletePositionCommand deletePositionCommand = getAnnotationCommandFactory()
					.newDeletePositionCommand();
			deletePositionCommand.setEditedBy(getEditedBy());
			deletePositionCommand.setPosition(actorPosition);
			getCommandHandler().execute(deletePositionCommand);
		} catch (NoSuchPositionException e) {
		}
		actor.getProjectOrDomain().getActors().remove(actor);
		getRepository().delete(actor);
	}

}
