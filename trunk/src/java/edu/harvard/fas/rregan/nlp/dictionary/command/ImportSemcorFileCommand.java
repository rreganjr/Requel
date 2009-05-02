/*
 * $Id: ImportSemcorFileCommand.java,v 1.1 2008/12/13 00:40:07 rregan Exp $
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
package edu.harvard.fas.rregan.nlp.dictionary.command;

import edu.harvard.fas.rregan.command.Command;
import edu.mit.jsemcor.element.IContext;
import edu.mit.jsemcor.main.IConcordance;

/**
 * Import a specific Semcor data file from the source files. Semcor is a
 * semantically annotated corpus based on the Brown corpus.
 * 
 * @author ron
 */
public interface ImportSemcorFileCommand extends Command {

	/**
	 * @param section -
	 *            the semcor section to load the file from.
	 */
	public void setSection(IConcordance section);

	/**
	 * @param context -
	 *            the context (file) to load.
	 */
	public void setContext(IContext context);
}