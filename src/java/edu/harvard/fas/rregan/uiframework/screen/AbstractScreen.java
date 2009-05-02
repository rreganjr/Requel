/*
 * $Id: AbstractScreen.java,v 1.5 2008/05/06 09:15:42 rregan Exp $
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

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.uiframework.UIFrameworkApp;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * This is the base class for ui elements that encompass the whole application
 * browser window.
 * 
 * @author ron
 */
public class AbstractScreen extends ContentPane implements Screen {
	private static final Logger log = Logger.getLogger(AbstractScreen.class);
	static final long serialVersionUID = 0L;

	private final List<ActionListener> actionListeners = new LinkedList<ActionListener>();
	private final ResourceBundleHelper resourceBundleHelper;

	protected AbstractScreen() {
		this(AbstractScreen.class.getName());
	}

	protected AbstractScreen(String resourceBundleName) {
		super();
		resourceBundleHelper = new ResourceBundleHelper(resourceBundleName);
	}

	public void addActionListener(ActionListener actionListener) {
		actionListeners.add(actionListener);
	}

	public void removeActionListener(ActionListener actionListener) {
		actionListeners.remove(actionListener);
	}

	protected ResourceBundleHelper getResourceBundleHelper(Locale locale) {
		resourceBundleHelper.setLocale(locale);
		return resourceBundleHelper;
	}

	public void actionPerformed(ActionEvent e) {
		for (ActionListener listener : actionListeners) {
			listener.actionPerformed(e);
		}
	}

	public EventDispatcher getEventDispatcher() {
		return getApp().getEventDispatcher();
	}

	public UIFrameworkApp getApp() {
		// this isn't set until after the window is created and
		// ManagerApp.init() is complete.
		// return (ManagerApp) getApplicationInstance();
		return UIFrameworkApp.getApp();
	}

	@Override
	public void dispose() {
		removeAll();
	}

	public void setup() {
	}
}
