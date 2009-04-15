/*
 * $Id: EditCommand.java,v 1.4 2008/12/13 00:40:51 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.command;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * An abstraction of a command where a user is creating or editing something.
 * 
 * @author ron
 */
public interface EditCommand extends Command {

	/**
	 * @param editedBy -
	 *            the user making the change to the object being edited
	 */
	public void setEditedBy(User editedBy);
}
