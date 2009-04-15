/*
 * $Id: MainScreenTabbedNavigation.java,v 1.8 2008/03/02 09:10:24 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
