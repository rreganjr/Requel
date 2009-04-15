/*
 * $Id: Annotation.java,v 1.10 2009/02/16 10:10:09 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
