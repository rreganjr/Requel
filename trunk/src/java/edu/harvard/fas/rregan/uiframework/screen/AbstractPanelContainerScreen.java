/*
 * $Id: AbstractPanelContainerScreen.java,v 1.2 2008/02/29 19:36:08 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.screen;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.PanelContainer;
import edu.harvard.fas.rregan.uiframework.panel.PanelManager;

/**
 * @author ron
 */
public abstract class AbstractPanelContainerScreen extends AbstractScreen implements PanelContainer {
	private static final Logger log = Logger.getLogger(AbstractPanelContainerScreen.class);
	static final long serialVersionUID = 0;

	private final PanelManager panelManager;

	/**
	 * 
	 */
	protected AbstractPanelContainerScreen(String resourceBundleName, PanelManager panelManager) {
		super(resourceBundleName);
		panelManager.setPanelContainer(this);
		this.panelManager = panelManager;
	}

	public PanelManager getPanelManager() {
		return panelManager;
	}
}
