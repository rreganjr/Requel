/*
 * $Id: EditChangeSpellingPositionCommand.java,v 1.4 2008/07/30 08:06:48 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
 * Elicitation System.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
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