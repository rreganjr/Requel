/*
 * $Id: AddActorToActorContainerCommandImpl.java,v 1.5 2009/03/30 11:54:29 rregan Exp $
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
import edu.harvard.fas.rregan.requel.project.command.AddActorToActorContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("addActorToActorContainerCommand")
@Scope("prototype")
public class AddActorToActorContainerCommandImpl extends AbstractEditProjectCommand implements
		AddActorToActorContainerCommand {

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 */
	@Autowired
	public AddActorToActorContainerCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository, 
			ProjectCommandFactory projectCommandFactory, AnnotationCommandFactory annotationCommandFactory,
			CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory, annotationCommandFactory, commandHandler);
	}

	private Actor actor;
	private ActorContainer actorContainer;

	@Override
	public Actor getActor() {
		return actor;
	}

	@Override
	public void setActor(Actor actor) {
		this.actor = actor;
	}

	@Override
	public ActorContainer getActorContainer() {
		return actorContainer;
	}

	@Override
	public void setActorContainer(ActorContainer actorContainer) {
		this.actorContainer = actorContainer;
	}

	@Override
	public void execute() {
		Actor addedActor = getProjectRepository().get(getActor());
		ActorContainer addingContainer = getProjectRepository().get(getActorContainer());
		addedActor.getReferers().add(addingContainer);
		addingContainer.getActors().add(addedActor);

		// replaced the supplied objects with the updated objects for retrieval.
		addedActor = getProjectRepository().merge(addedActor);
		addingContainer = getProjectRepository().merge(addingContainer);
		setActor(addedActor);
		setActorContainer(addingContainer);
	}
}
