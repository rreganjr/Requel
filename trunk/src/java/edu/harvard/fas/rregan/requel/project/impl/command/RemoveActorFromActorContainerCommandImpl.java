/*
 * $Id: RemoveActorFromActorContainerCommandImpl.java,v 1.5 2009/03/30 11:54:29 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.ActorContainer;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.command.RemoveActorFromActorContainerCommand;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.UserRepository;

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
			ProjectCommandFactory projectCommandFactory, AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory, annotationCommandFactory, commandHandler);
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
