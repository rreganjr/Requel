/*
 * $Id: EditPositionCommand.java,v 1.2 2008/05/22 09:26:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.command;

import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.command.EditCommand;

/**
 * Create or edit a position for an issue.
 * 
 * @author ron
 */
public interface EditPositionCommand extends EditCommand {

	/**
	 * Set the issue for a new position to be attached to.
	 * 
	 * @param issue
	 */
	public void setIssue(Issue issue);

	/**
	 * Set the position to edit.
	 * 
	 * @param position
	 */
	public void setPosition(Position position);

	/**
	 * Get the new or updated position.
	 * 
	 * @return
	 */
	public Position getPosition();

	/**
	 * Set the text for the position.
	 * 
	 * @param text
	 */
	public void setText(String text);

}
