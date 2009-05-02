/*
 * $Id: PanelContainer.java,v 1.5 2008/02/29 19:36:09 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework;

import edu.harvard.fas.rregan.uiframework.navigation.WorkflowDisposition;
import edu.harvard.fas.rregan.uiframework.panel.Panel;
import edu.harvard.fas.rregan.uiframework.panel.PanelManager;

/**
 * A PanelContainer manages how a set of related panels managed by the container
 * are displayed. A PanelContainer has a PanelManager that manages the creation
 * and state of panels based on NavigationEvents it receives from the
 * EventDisplatcher.
 * 
 * @author ron
 */
public interface PanelContainer extends Panel {

	/**
	 * Display the supplied panel.
	 * 
	 * @param panel
	 * @param disposition
	 */
	public void displayPanel(Panel panel, WorkflowDisposition disposition);

	/**
	 * Hide or remove the supplied panel.
	 * 
	 * @param panel
	 */
	public void undisplayPanel(Panel panel);

	/**
	 * This is public so that the NavigationEventHandlers can get the panel to
	 * forward the event to.
	 * 
	 * @return
	 */
	public PanelManager getPanelManager();
}
