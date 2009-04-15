/*
 * $Id: EditActorCommand.java,v 1.5 2009/01/27 09:30:16 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import java.util.Set;

import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.ActorContainer;

/**
 * @author ron
 */
public interface EditActorCommand extends EditTextEntityCommand {

	/**
	 * Set the actor to edit.
	 * 
	 * @param actor
	 */
	public void setActor(Actor actor);

	/**
	 * Get the new or updated actor.
	 * 
	 * @return
	 */
	public Actor getActor();

	/**
	 * set a single container. helper that creates a set and adds the single
	 * container to it and calls setActorContainers().
	 * 
	 * @param actorContainer
	 */
	public void setActorContainer(ActorContainer actorContainer);

	/**
	 * Set the actor containers that refer to the actor. This is the absolute
	 * set of referers.<br>
	 * Either this or setAddReferers should be used, this takes precedence.
	 * 
	 * @param referers
	 */
	public void setActorContainers(Set<ActorContainer> referers);

	/**
	 * Set the actor containers to add as referers to the actor.<br>
	 * Either this or setReferers should be used, setReferers takes precedence.
	 * 
	 * @param actorContainers
	 */
	public void setAddActorContainers(Set<ActorContainer> actorContainers);
}
