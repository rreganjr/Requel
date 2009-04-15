/*
 * $Id: EditAnnotationCommand.java,v 1.1 2008/09/04 09:47:19 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.annotation.command;

import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.command.EditCommand;

/**
 * Base class for annotation editing commands.
 * 
 * @author ron
 */
public interface EditAnnotationCommand extends EditCommand {

	/**
	 * Set the object used for grouping the annotation.
	 * 
	 * @param groupingObject -
	 *            a persistent object
	 */
	public void setGroupingObject(Object groupingObject);

	/**
	 * Set the text for the issue.
	 * 
	 * @param text
	 */
	public void setText(String text);

	/**
	 * Set the thing being annotated with the issue.
	 * 
	 * @param annotatable -
	 *            set the entity being annotated.
	 */
	public void setAnnotatable(Annotatable annotatable);
}
