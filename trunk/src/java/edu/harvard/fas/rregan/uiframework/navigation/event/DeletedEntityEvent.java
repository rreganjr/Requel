/*
 * $Id: DeletedEntityEvent.java,v 1.1 2009/02/15 09:31:37 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.navigation.event;

import edu.harvard.fas.rregan.uiframework.panel.Panel;

/**
 * @author ron
 */
public class DeletedEntityEvent extends UpdateEntityEvent {
	static final long serialVersionUID = 0;

	/**
	 * @param source
	 * @param panelToClose
	 * @param updatedObject
	 */
	public DeletedEntityEvent(Object source, Panel panelToClose, Object updatedObject) {
		super(source, panelToClose, updatedObject);
	}

	/**
	 * @param source
	 * @param command
	 * @param panelToClose
	 * @param updatedObject
	 */
	public DeletedEntityEvent(Object source, String command, Panel panelToClose,
			Object updatedObject) {
		super(source, command, panelToClose, updatedObject);
	}

	/**
	 * @param source
	 * @param updatedObject
	 */
	public DeletedEntityEvent(Panel source, Object updatedObject) {
		super(source, updatedObject);
	}
}
