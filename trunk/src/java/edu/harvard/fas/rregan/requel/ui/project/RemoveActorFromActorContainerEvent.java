/*
 * $Id: RemoveActorFromActorContainerEvent.java,v 1.1 2008/09/12 22:44:18 rregan Exp $
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
