/*
 * $Id: AbstractPanelContainerPanel.java,v 1.4 2008/02/29 19:36:12 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
