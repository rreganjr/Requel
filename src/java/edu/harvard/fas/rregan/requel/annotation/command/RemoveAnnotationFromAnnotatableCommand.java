/*
 * $Id: RemoveAnnotationFromAnnotatableCommand.java,v 1.2 2009/01/19 09:32:22 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.annotation.command;

import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.command.EditCommand;

/**
 * @author ron
 */
public interface RemoveAnnotationFromAnnotatableCommand extends EditCommand {

	/**
	 * Set the Annotation to remove.
	 * 
	 * @param annotation
	 */
	public void setAnnotation(Annotation annotation);

	/**
	 * @return the updated Annotation.
	 */
	public Annotation getAnnotation();

	/**
	 * Set the annotatable the Annotation is being removed from.
	 * 
	 * @param annotatable
	 */
	public void setAnnotatable(Annotatable annotatable);

	/**
	 * @return the updated Annotatable container.
	 */
	public Annotatable getAnnotatable();
}
