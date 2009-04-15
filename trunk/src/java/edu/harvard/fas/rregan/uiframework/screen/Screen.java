/*
 * $Id: Screen.java,v 1.2 2008/02/27 08:04:16 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.screen;

import nextapp.echo2.app.event.ActionListener;
import edu.harvard.fas.rregan.uiframework.UIFrameworkApp;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * @author ron
 */
public interface Screen extends ActionListener {

	public void addActionListener(ActionListener actionListener);

	public void removeActionListener(ActionListener actionListener);

	public EventDispatcher getEventDispatcher();

	public UIFrameworkApp getApp();

	/**
	 * This method gets called by the UIFrameworkApp setCurrentScreen() when a
	 * new screen replaces the original screen.<br/>NOTE: This method should
	 * not be called manually.
	 */
	public void dispose();

	/**
	 * This method gets called by the UIFrameworkApp setCurrentScreen() when a
	 * new screen is set as the current screen.<br/>NOTE: This method should
	 * not be called manually.
	 */
	public void setup();
}
