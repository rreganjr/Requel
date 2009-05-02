/*
 * $Id: PanelManager.java,v 1.3 2008/03/10 23:57:20 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.panel;

import java.util.Set;

import nextapp.echo2.app.event.ActionListener;
import edu.harvard.fas.rregan.uiframework.PanelContainer;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * A PanelManager manages the creation and state of panels displayed in a
 * PanelContainer. A PanelManager listens for navigation events from the
 * EventDispatcher concerning panels in the PanelContainer and call methods on
 * the container to alter the display. A PanelManager is only assigned to a
 * single PanelContainer. <br>
 * NOTE: In cases where multiple PanelContainers are used, the developer is
 * responsible for ensuring that two different managers aren't configured to
 * display the same panels or listen for the same events.
 * 
 * @author ron
 */
public interface PanelManager extends ActionListener {

	/**
	 * Assign a PanelContainer to the manager for sending events to.
	 * 
	 * @param panelContainer
	 */
	public void setPanelContainer(PanelContainer panelContainer);

	/**
	 * Register a Panel or PanelFactory so that it is available for use in
	 * response to an event.
	 * 
	 * @param panelDescriptor
	 * @param eventDispatcher
	 */
	public void register(PanelDescriptor panelDescriptor, EventDispatcher eventDispatcher);

	/**
	 * Register a set of Panels or PanelFactories.
	 * 
	 * @param panelDescriptor
	 * @param eventDispatcher
	 */
	public void register(Set<PanelDescriptor> panelDescriptor, EventDispatcher eventDispatcher);

	/**
	 * Cleanup any cached panels that the manager is holding by calling
	 * dispose() on them.
	 */
	public void dispose();
}