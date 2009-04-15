/*
 * $Id: EditNoteCommand.java,v 1.2 2008/09/04 09:47:19 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.command;

import edu.harvard.fas.rregan.requel.annotation.Note;

/**
 * Create or edit a note on an annotatable entity.
 * 
 * @author ron
 */
public interface EditNoteCommand extends EditAnnotationCommand {

	/**
	 * Set the note to edit.
	 * 
	 * @param note
	 */
	public void setNote(Note note);

	/**
	 * Get the new or updated note.
	 * 
	 * @return
	 */
	public Note getNote();
}
