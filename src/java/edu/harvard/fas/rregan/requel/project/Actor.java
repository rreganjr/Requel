/*
 * $Id: Actor.java,v 1.4 2008/09/06 09:31:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

import java.util.Set;

/**
 * @author ron
 */
public interface Actor extends TextEntity, GoalContainer, Comparable<Actor> {

	/**
	 * @return The referers/users of the actor.
	 */
	public Set<ActorContainer> getReferers();

}
