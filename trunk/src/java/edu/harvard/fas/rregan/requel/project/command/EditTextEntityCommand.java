/*
 * $Id: EditTextEntityCommand.java,v 1.1 2009/01/27 09:30:16 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

/**
 * @author ron
 */
public interface EditTextEntityCommand extends EditProjectOrDomainEntityCommand {

	/**
	 * The name of the "text" field used to correlate to the field in an editor
	 * and through exceptions.
	 */
	public static final String FIELD_TEXT = "text";

	/**
	 * Set the text for the entity.
	 * 
	 * @param text
	 */
	public void setText(String text);
}
