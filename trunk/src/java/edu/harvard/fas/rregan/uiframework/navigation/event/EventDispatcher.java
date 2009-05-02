/*
 * $Id: EventDispatcher.java,v 1.6 2008/09/12 00:15:11 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.navigation.event;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import edu.harvard.fas.rregan.uiframework.panel.Panel;
import edu.harvard.fas.rregan.uiframework.panel.PanelDescriptor;

/**
 * The EventDispatcher is a broker for events. Listeners can be registered for
 * specific types of events, open panel events for specific types of panels, and
 * for all events to specific panel instances.<br>
 * PanelManagers use the add/remove methods to
 * 
 * @author ron
 */
public interface EventDispatcher extends ActionListener {

	/**
	 * @param event
	 */
	public void dispatchEvent(ActionEvent event);

	/**
	 * Add a listener for a specific type of event or any sub class of that
	 * event.<br>
	 * This method is most often used by Controllers such as the LoginController
	 * or simple Listeners such as NavigatorTreeNodeUpdateListener that listens
	 * for entity changes and updates a navigation tree.
	 * 
	 * @param eventType -
	 *            the class of an ActionEvent or sub class to listen for.
	 * @param listener -
	 *            a Controller or simple listener that will receive the event
	 *            when dispatched through the dispatchEvent() method.
	 * @return the supplied listener
	 */
	public ActionListener addEventTypeActionListener(Class<? extends ActionEvent> eventType,
			ActionListener listener);

	/**
	 * Add a listener for a specific type of event or any sub class of that
	 * event and a specific destination, such as a select button.
	 * 
	 * @param eventType -
	 *            the class of an ActionEvent or sub class to listen for.
	 * @param listener -
	 *            a Controller or simple listener that will receive the event
	 *            when dispatched through the dispatchEvent() method.
	 * @param destinationObject -
	 *            the destination object. Events will only get fired to the
	 *            listener if the destination of the event matches the
	 *            registered destination.
	 * @return the supplied listener
	 */
	public ActionListener addEventTypeActionListener(Class<? extends ActionEvent> eventType,
			ActionListener listener, Object destinationObject);

	/**
	 * Remove a listener for a specific type of event.
	 * 
	 * @param eventType -
	 *            the class of an ActionEvent or sub class to listen for.
	 * @param listener -
	 *            the Controller or simple listener that was registered to
	 *            receive the events and will be removed.
	 */
	public void removeEventTypeActionListener(Class<? extends ActionEvent> eventType,
			ActionListener listener);

	/**
	 * Remove a listener for a specific type of event and destination. Listeners
	 * registered with the addEventTypeActionListener() that takes a
	 * destinationObject should be removed with this method.
	 * 
	 * @param eventType -
	 *            the class of an ActionEvent or sub class to listen for.
	 * @param listener -
	 *            the Controller or simple listener that was registered to
	 *            receive the events and will be removed.
	 * @param destinationObject -
	 *            the destination object the listener is registered with.
	 */
	public void removeEventTypeActionListener(Class<? extends ActionEvent> eventType,
			ActionListener listener, Object destinationObject);

	/**
	 * Add a listener for an OpenPanelEvent for the specified PanelDescriptor.
	 * When an OpenPanelEvent is dispatched that matches the panelDescriptor,
	 * the specified listener will be notified.
	 * 
	 * @param panelDescriptor
	 * @param listener
	 * @return the supplied listener
	 */
	public ActionListener addOpenPanelEventActionListener(PanelDescriptor panelDescriptor,
			ActionListener listener);

	/**
	 * Remove a listener for an OpenPanelEvent for the specified
	 * PanelDescriptor.
	 * 
	 * @param panelDescriptor
	 * @param listener
	 */
	public void removeOpenPanelEventActionListener(PanelDescriptor panelDescriptor,
			ActionListener listener);

	/**
	 * Add a listener for all events to a specific panel. The PanelManager
	 * should call this each time a panel is opened so that when it is hidden
	 * and re-displayed the manager is notified.
	 * 
	 * @param panel
	 * @param listener
	 * @return the supplied listener
	 */
	public ActionListener addPanelInstanceEventActionListener(Panel panel, ActionListener listener);

	/**
	 * Remove a listener for the panel for close events. The PanelManager should
	 * call this each time a panel is closed.
	 * 
	 * @param panel
	 * @param listener
	 */
	public void removePanelInstanceEventActionListener(Panel panel, ActionListener listener);
}
