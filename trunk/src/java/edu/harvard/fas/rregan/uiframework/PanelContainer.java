/*
 * $Id: PanelContainer.java,v 1.5 2008/02/29 19:36:09 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
