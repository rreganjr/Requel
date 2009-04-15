/*
 * $Id: AddActorToActorContainerCommand.java,v 1.1 2008/09/06 09:31:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.ActorContainer;

/**
 * @author ron
 */
public interface AddActorToActorContainerCommand extends EditCommand {

	/**
	 * Set the Story to add.
	 * 
	 * @param actor
	 */
	public void setActor(Actor actor);

	/**
	 * @return the updated actor.
	 */
	public Actor getActor();

	/**
	 * Set the container this actor is being added to.
	 * 
	 * @param actorContainer
	 */
	public void setActorContainer(ActorContainer actorContainer);

	/**
	 * @return the updated actor container.
	 */
	public ActorContainer getActorContainer();
}
