/*
 * $Id: EditChangeSpellingPositionCommand.java,v 1.4 2008/07/30 08:06:48 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.command;

/**
 * @author ron
 */
public interface EditChangeSpellingPositionCommand extends EditPositionCommand {

	/**
	 * Set the proposed word to replace the misspelled word in the annotatable
	 * entity.
	 * 
	 * @param proposedWord -
	 *            the proposed word to correct the word in the annotatable
	 *            entity.
	 */
	public void setProposedWord(String proposedWord);
}
