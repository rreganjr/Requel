/*
 * $Id: DeleteNoteCommand.java,v 1.1 2009/02/13 12:08:00 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.command;

import edu.harvard.fas.rregan.requel.annotation.Note;
import edu.harvard.fas.rregan.requel.command.EditCommand;

/**
 * Delete a note from the system. Update all the annotatables that refer to the
 * note and remove the reference.
 * 
 * @author ron
 */
public interface DeleteNoteCommand extends EditCommand {

	/**
	 * Set the note to delete.
	 * 
	 * @param note
	 */
	public void setNote(Note note);
}
