/*
 * $Id: ClosePanelEvent.java,v 1.7 2008/09/12 01:00:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
