/*
 * $Id: UIFrameworkConfiguration.java,v 1.3 2008/02/19 09:48:45 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework;

import nextapp.echo2.app.StyleSheet;
import nextapp.echo2.app.componentxml.ComponentXmlException;
import nextapp.echo2.app.componentxml.StyleSheetLoader;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.login.LoginScreen;
import edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.screen.Screen;

/**
 * TODO: the configuration should be replaced by Spring AutoWiring of resources
 * 
 * @author ron
 */
public class UIFrameworkConfiguration {
	private static final Logger log = Logger.getLogger(UIFrameworkConfiguration.class);

	private Screen initialScreen;
	private EventDispatcher eventDispatcher;
	private StyleSheet styleSheet;
	private String mainWindowTitle;

	/**
	 * TODO: place holder configuration
	 */
	public UIFrameworkConfiguration() {
		try {
			mainWindowTitle = "Title";
			initialScreen = new LoginScreen();
			eventDispatcher = new DefaultEventDispatcher();
			styleSheet = StyleSheetLoader.load("Default.stylesheet", Thread.currentThread()
					.getContextClassLoader());
		} catch (ComponentXmlException e) {
			throw UIFrameworkException.errorInStyleSheet(e.getMessage());
		} catch (Exception e) {
			throw UIFrameworkException.exceptionInConfig(e);
		}
		log.debug("new UIFrameworkConfiguration");
	}

	/**
	 * @return
	 */
	public String getMainWindowTitle() {
		return mainWindowTitle;
	}

	/**
	 * @return
	 */
	public Screen getInitialScreen() {
		return initialScreen;
	}

	/**
	 * @return
	 */
	public EventDispatcher getEventDispatcher() {
		return eventDispatcher;
	}

	public StyleSheet getStyleSheet() {
		return styleSheet;
	}

	/**
	 * TODO: this should be replaced by Spring AutoWiring of Commands
	 * 
	 * @param app
	 */
	public void registerCommands(UIFrameworkApp app) {
	}
}
