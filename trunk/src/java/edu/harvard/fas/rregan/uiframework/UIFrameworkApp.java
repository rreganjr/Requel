/*
 * $Id$
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

import java.util.Map;
import java.util.Set;

import nextapp.echo2.app.ApplicationInstance;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.StyleSheet;
import nextapp.echo2.app.TaskQueueHandle;
import nextapp.echo2.app.Window;
import nextapp.echo2.app.componentxml.ComponentXmlException;
import nextapp.echo2.app.componentxml.StyleSheetLoader;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.webcontainer.ContainerContext;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import edu.harvard.fas.rregan.uiframework.controller.AppAwareController;
import edu.harvard.fas.rregan.uiframework.controller.Controller;
import edu.harvard.fas.rregan.uiframework.login.LoginScreen;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.screen.Screen;

/**
 * @author ron
 */
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
public class UIFrameworkApp extends ApplicationInstance {
	private static final Logger log = Logger.getLogger(UIFrameworkApp.class);
	static final long serialVersionUID = 0;

	private Window mainWindow;
	private final EventDispatcher eventDispatcher;
	private final Map<String, Screen> screens;
	private Screen currentScreen;
	private Object user;
	private StyleSheet styleSheet;

	/**
	 * @param eventDispatcher
	 * @param controllers
	 * @param screens
	 * @param initialScreenName
	 */
	@Autowired
	public UIFrameworkApp(EventDispatcher eventDispatcher, Set<Controller> controllers,
			Map<String, Screen> screens) {
		super();
		log.debug("new UIFrameworkApp");
		this.eventDispatcher = eventDispatcher;
		this.screens = screens;

		// TODO: setup of controllers can go away if the app scope is changed to
		// session and it
		// is injected into each AppAwareController, and the eventDispatcher is
		// injected into
		// each controller. The controllers would still need to be autowired and
		// injected into
		// this method so that they get instantiated or else they need to be
		// manually added to
		// the spring config.

		// register the events the UIFrameworkApp
		// register the controller with the dispatcher
		for (Controller controller : controllers) {
			// register the EventDispatcher as a listener on the controller so
			// it can dispatch the controllers events
			controller.addActionListener(eventDispatcher);

			if (controller instanceof AppAwareController) {
				((AppAwareController) controller).setApp(this);
			}
			if (controller.getEventTypesToListenFor().size() > 0) {
				for (Class<? extends ActionEvent> eventType : controller.getEventTypesToListenFor()) {
					eventDispatcher.addEventTypeActionListener(eventType, controller);
				}
			} else {
				log.warn("controller '" + controller
						+ "' does not have any event types to listen for.");
			}
		}

		if (log.isDebugEnabled()) {
			for (String screenName : screens.keySet()) {
				log.debug(screenName + ": " + screens.get(screenName).getClass().getName());
			}
		}
		try {
			styleSheet = StyleSheetLoader.load("Default.stylesheet", Thread.currentThread()
					.getContextClassLoader());
		} catch (ComponentXmlException e) {
			throw UIFrameworkException.errorInStyleSheet(e.getMessage());
		}
	}

	/**
	 * @return
	 */
	public EventDispatcher getEventDispatcher() {
		return eventDispatcher;
	}

	@Override
	public Window init() {
		setStyleSheet(styleSheet);
		mainWindow = new Window();
		mainWindow.setTitle("Title");
		setCurrentScreen(LoginScreen.screenName);
		return mainWindow;
	}

	/**
	 * @param screenName
	 */
	public void setCurrentScreen(String screenName) {
		if (screens.containsKey(screenName)) {
			Screen newScreen = screens.get(screenName);

			// replace the currentScreen in the window
			ContentPane mainContent = mainWindow.getContent();
			// this removes and disposes all components of the last screen.
			mainContent.removeAll();
			mainContent.add((Component) newScreen);

			newScreen.setup();
			currentScreen = newScreen;
		} else {
			log.warn("specified screen '" + screenName + "' does not exist");
		}
	}

	/**
	 * @return
	 */
	public Object getUser() {
		return user;
	}

	/**
	 * @param user
	 */
	public void setUser(Object user) {
		this.user = user;
	}

	/**
	 * Add a task to Echo that causes the client to poll back to the server in
	 * pollTimeMillis milliseconds and execute the task and remove it from the
	 * queue.
	 * 
	 * @see RepeatingTask - a task that reregisters it self to have the client
	 *      repeat polling until the task is explicitly stopped via stopTask()
	 * @see AvailableJobsPanel for an example
	 * @param taskQueue -
	 *            an object returned by getApp().createTaskQueue().
	 * @param task -
	 *            a Runnable object like
	 * @see RepeatingTask
	 * @param pollTimeMillis -
	 *            the time in milliseconds to wait before polling
	 */
	public void enqueueTask(TaskQueueHandle taskQueue, Runnable task, int pollTimeMillis) {
		enqueueTask(taskQueue, task);
		// set the timeout in the container context
		ContainerContext containerContext = (ContainerContext) getContextProperty(ContainerContext.CONTEXT_PROPERTY_NAME);
		containerContext.setTaskQueueCallbackInterval(taskQueue, pollTimeMillis);
	}

	/**
	 * @return the current app instance relative to the session for the current
	 *         user
	 */
	public static UIFrameworkApp getApp() {
		return (UIFrameworkApp) getActive();
	}
}
