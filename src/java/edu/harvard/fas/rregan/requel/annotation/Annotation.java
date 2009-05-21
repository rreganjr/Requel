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
package edu.harvard.fas.rregan.requel.annotation;

import java.util.Set;

import edu.harvard.fas.rregan.requel.CreatedEntity;
import edu.harvard.fas.rregan.requel.Describable;

/**
 * An abstraction of something that can be added to an Annotatable object such
 * as an Issue or Note. An annotation may itself be annotated.
 * 
 * @author ron
 */
public interface Annotation extends Comparable<Annotation>, CreatedEntity, Describable {

	/**
	 * @return an object used as a context for a group of annotations.
	 */
	public Object getGroupingObject();

	/**
	 * @return the simple name of the type of annotation
	 */
	public String getTypeName();

	/**
	 * @return The annotatable objects that have this annotation attached.
	 */
	public Set<Annotatable> getAnnotatables();

	/**
	 * @return The text of the annotation.
	 */
	public String getText();

	/**
	 * @return return true if this issue must be resolved.
	 */
	public boolean isMustBeResolved();

	/**
	 * @return return true if this issue has been resolved.
	 */
	public boolean isResolved();

	/**
	 * @return a message appropriate for the state of the annotation.
	 */
	public String getStatusMessage();
}
