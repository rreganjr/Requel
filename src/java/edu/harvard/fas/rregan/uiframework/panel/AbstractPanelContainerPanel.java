/*
 * $Id: AbstractPanelContainerPanel.java,v 1.4 2008/02/29 19:36:12 rregan Exp $
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

import edu.harvard.fas.rregan.uiframework.PanelContainer;

/**
 * @author ron
 */
public abstract class AbstractPanelContainerPanel extends AbstractPanel implements PanelContainer {

	private final PanelManager panelManager;

	/**
	 * @param resourceBundleName
	 * @param panelManager
	 */
	protected AbstractPanelContainerPanel(String resourceBundleName, PanelManager panelManager) {
		super(resourceBundleName, PanelActionType.Unspecified, Object.class);
		panelManager.setPanelContainer(this);
		this.panelManager = panelManager;
	}

	/**
	 * The PanelManager manages the creation and state of panels displayed in
	 * the container while the PanelContainer manages how the panels are
	 * displayed. The PanelManager will listen for navigation events from the
	 * EventDispatcher concerning panels in the container and call methods on
	 * the container to alter the display. Each container will have its own
	 * manager instance.
	 * 
	 * @return the manager for this panel
	 */
	// This is public so that the NavigationEventHandlers
	public PanelManager getPanelManager() {
		return panelManager;
	}
}
