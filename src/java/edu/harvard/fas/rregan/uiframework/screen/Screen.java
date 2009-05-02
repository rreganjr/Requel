/*
 * $Id: Screen.java,v 1.2 2008/02/27 08:04:16 rregan Exp $
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
