/*
 * $Id: EditArgumentCommand.java,v 1.3 2009/02/17 11:50:48 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.command;

import edu.harvard.fas.rregan.requel.annotation.Argument;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.command.EditCommand;

/**
 * Create or edit an argument for a position of an issue.
 * 
 * @author ron
 */
public interface EditArgumentCommand extends EditCommand {

	/**
	 * Set the argument to edit.
	 * 
	 * @param argument
	 */
	public void setArgument(Argument argument);

	/**
	 * @return the new or updated argument.
	 */
	public Argument getArgument();

	/**
	 * The position that the argument is for or against.
	 * 
	 * @param position
	 */
	public void setPosition(Position position);

	/**
	 * Set the text for the argument.
	 * 
	 * @param text
	 */
	public void setText(String text);

	/**
	 * Set the support level of the argument for or against the position.
	 * 
	 * @param supportLevelName
	 */
	public void setSupportLevelName(String supportLevelName);
}
