/*
 * $Id: UIFrameworkConfiguration.java,v 1.3 2008/02/19 09:48:45 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
