/*
 * $Id: WorkflowDisposition.java,v 1.2 2008/02/27 11:34:41 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.navigation;

/**
 * WorkflowDisposition indicates the relationship between panels for a set of
 * work. It is used by events and panel management
 * 
 * @author ron
 */
public enum WorkflowDisposition {

	/**
	 * If the disposition of an event is NewFlow then the PanelManager should
	 * cleanup the state of the previous workflow checking for unsaved data and
	 * allowing the user to save or throw away any changes.
	 */
	NewFlow(),

	/**
	 * If the disposition of an event is ContinueFlow, it indicates to the
	 * PanelManager that the state of the last window should be preservied while
	 * the user does additional work in the new panel. When the user is finished
	 * working in the new window, the previous window will be displayed.
	 */
	ContinueFlow();

	private WorkflowDisposition() {
	}
}
