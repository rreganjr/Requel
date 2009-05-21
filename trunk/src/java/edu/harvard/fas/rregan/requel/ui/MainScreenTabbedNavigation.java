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
package edu.harvard.fas.rregan.requel.ui;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.panel.PanelManager;
import edu.harvard.fas.rregan.uiframework.panel.TabbedPanelContainer;

/**
 * @author ron
 */
public class MainScreenTabbedNavigation extends TabbedPanelContainer {
	private static final Logger log = Logger.getLogger(MainScreenTabbedNavigation.class);
	static final long serialVersionUID = 0;

	/**
	 * The style name to use in the stylesheet to configure the tabs.
	 */
	public static final String STYLE_NAME_NAV_PANEL_CONTAINER = "RequelMainScreen.MainScreenTabbedNavigation";

	/**
	 * @param navPanels
	 * @param panelManager
	 */
	public MainScreenTabbedNavigation(PanelManager panelManager) {
		super(MainScreenTabbedNavigation.class.getName(), panelManager);

	}

	@Override
	public void setup() {
		super.setup();
		setStyleName(STYLE_NAME_NAV_PANEL_CONTAINER);
	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
	}
}
