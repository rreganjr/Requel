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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ActorContainer;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RemoveActorFromActorContainerCommand;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("removeActorFromActorContainerCommand")
@Scope("prototype")
public class RemoveActorFromActorContainerCommandImpl extends AbstractEditProjectCommand implements
		RemoveActorFromActorContainerCommand {

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 */
	@Autowired
	public RemoveActorFromActorContainerCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	private Actor actor;
	private ActorContainer actorContainer;

	@Override
	public Actor getActor() {
		return actor;
	}

	@Override
	public ActorContainer getActorContainer() {
		return actorContainer;
	}

	@Override
	public void setActor(Actor actor) {
		this.actor = actor;
	}

	@Override
	public void setActorContainer(ActorContainer actorContainer) {
		this.actorContainer = actorContainer;
	}

	@Override
	public void execute() {
		Actor removedActor = getProjectRepository().get(getActor());
		ActorContainer removingContainer = getProjectRepository().get(getActorContainer());
		removedActor.getReferers().remove(removingContainer);
		removingContainer.getActors().remove(removedActor);

		// replaced the supplied objects with the updated objects for retrieval.
		removedActor = getRepository().merge(removedActor);
		removingContainer = getRepository().merge(removingContainer);
		setActor(removedActor);
		setActorContainer(removingContainer);
	}
}
