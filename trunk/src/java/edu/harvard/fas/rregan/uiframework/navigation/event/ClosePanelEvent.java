/*
 * $Id$
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
 * An event that causes a specified panel to be closed.
 * 
 * @author ron
 */
public class ClosePanelEvent extends NavigationEvent {
	static final long serialVersionUID = 0;

	private final Panel panelToClose;

	public ClosePanelEvent(Panel panelToClose) {
		this(panelToClose, panelToClose);
	}

	/**
	 * @param source
	 * @param panelToClose
	 */
	public ClosePanelEvent(Object source, Panel panelToClose) {
		this(source, ClosePanelEvent.class.getName(), panelToClose, null);
	}

	protected ClosePanelEvent(Object source, String command, Panel panelToClose,
			Object destinationObject) {
		super(source, command, destinationObject);
		this.panelToClose = panelToClose;
	}

	/**
	 * @return the panel that should be closed.
	 */
	public Panel getPanelToClose() {
		return panelToClose;
	}
}
