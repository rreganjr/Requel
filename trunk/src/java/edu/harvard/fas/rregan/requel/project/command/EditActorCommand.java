/*
 * $Id$
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
