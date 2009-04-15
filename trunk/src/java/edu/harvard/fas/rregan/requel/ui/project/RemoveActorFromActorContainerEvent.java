/*
 * $Id: RemoveActorFromActorContainerEvent.java,v 1.1 2008/09/12 22:44:18 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.ui.project;

import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.ActorContainer;
import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;

/**
 * This event is fired from the ActorsTable when a user clicks the "remove"
 * button to remove the actor from the actor container.
 * 
 * @author ron
 */
public class RemoveActorFromActorContainerEvent extends NavigationEvent {
	static final long serialVersionUID = 0;

	private final Actor actor;
	private final ActorContainer actorContainer;

	/**
	 * @param source
	 * @param actor
	 * @param actorContainer
	 * @param destinationObject
	 */
	public RemoveActorFromActorContainerEvent(Object source, Actor actor,
			ActorContainer actorContainer, Object destinationObject) {
		super(source, RemoveActorFromActorContainerEvent.class.getName(), destinationObject);
		this.actor = actor;
		this.actorContainer = actorContainer;
	}

	/**
	 * @return the actor to remove from the container.
	 */
	public Actor getActor() {
		return actor;
	}

	/**
	 * @return the container to remove the actor from.
	 */
	public ActorContainer getActorContainer() {
		return actorContainer;
	}
}
