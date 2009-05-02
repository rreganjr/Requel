/*
 * $Id: EditTextEntityCommand.java,v 1.1 2009/01/27 09:30:16 rregan Exp $
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
