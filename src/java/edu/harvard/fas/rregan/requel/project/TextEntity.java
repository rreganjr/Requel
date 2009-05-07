/*
 * $Id: TextEntity.java,v 1.3 2008/03/19 09:57:31 rregan Exp $
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
package edu.harvard.fas.rregan.requel.project;

/**
 * An entity whose primary content is text, such as a goal or story.
 * 
 * @author ron
 */
public interface TextEntity extends ProjectOrDomainEntity {

	/**
	 * @return get the text of this entity.
	 */
	public String getText();

	/**
	 * change the text of this entity.
	 * 
	 * @param text -
	 *            new text
	 */
	public void setText(String text);
}