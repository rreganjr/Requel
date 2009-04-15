/*
 * $Id: ActorContainer.java,v 1.4 2008/09/06 09:31:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

import java.util.Comparator;
import java.util.Set;

import edu.harvard.fas.rregan.requel.CreatedEntity;
import edu.harvard.fas.rregan.requel.Describable;

/**
 * An abstraction of something that contains a set of Actors.
 * 
 * @author ron
 */
public interface ActorContainer extends Describable, CreatedEntity {

	/**
	 * @return the actors referenced by this container
	 */
	public Set<Actor> getActors();

	/**
	 * Compare the objects that contain Storys by the description.
	 */
	public static final Comparator<ActorContainer> COMPARATOR = new ActorContainerComparator();

	/**
	 * A Comparator for collections of Story containers.
	 */
	public static class ActorContainerComparator implements Comparator<ActorContainer> {
		@Override
		public int compare(ActorContainer o1, ActorContainer o2) {
			return o1.getDescription().compareTo(o2.getDescription());
		}
	}

}
