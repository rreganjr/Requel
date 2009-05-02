/*
 * $Id: AbstractController.java,v 1.8 2009/01/27 09:30:17 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * A base implementation for a Controller that contains the management for
 * action listeners that listen for events from this controller (typically the
 * event dispatcher) and the event types that this controller is interested in
 * recieving (as an action listener itself.)<br>
 * Classes that extend this class should call addEventTypeToListenFor() in the
 * constructor for each event this controller handles so that it will be wired
 * up to receive those events from the event dispatcher.
 * 
 * @author ron
 */
public abstract class AbstractController implements Controller {
	private static final Logger log = Logger.getLogger(AbstractController.class);

	// TODO: use a weak hash so stale listeners are removed?
	private final Set<ActionListener> actionListeners = new HashSet<ActionListener>();
	private final Set<Class<? extends ActionEvent>> eventTypesToListenFor = new HashSet<Class<? extends ActionEvent>>();
	private EventDispatcher eventDispatcher = null;

	protected AbstractController() {
		super();
		log.debug("creating controller " + getClass().getName());
	}

	protected AbstractController(EventDispatcher eventDispatcher) {
		this();
		setEventDispatcher(eventDispatcher);
	}

	protected EventDispatcher getEventDispatcher() {
		return eventDispatcher;
	}

	protected void setEventDispatcher(EventDispatcher eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
		if (eventDispatcher != null) {
			addActionListener(eventDispatcher);
		}
	}

	protected Set<ActionListener> getListeners() {
		return actionListeners;
	}

	public void addActionListener(ActionListener actionListener) {
		log.debug("adding listener: " + actionListener);
		getListeners().add(actionListener);
	}

	public void removeActionListener(ActionListener actionListener) {
		log.debug("removing listener: " + actionListener);
		getListeners().remove(actionListener);
	}

	protected void fireEvent(ActionEvent event) {
		log.debug("event: " + event);
		if (event != null) {
			for (ActionListener listener : getListeners()) {
				log.debug("sending event to listener " + listener);
				listener.actionPerformed(event);
			}
		}
	}

	protected void addEventTypeToListenFor(Class<? extends ActionEvent> eventType) {
		eventTypesToListenFor.add(eventType);
	}

	protected void removeEventTypeToListenFor(Class<? extends ActionEvent> eventType) {
		eventTypesToListenFor.remove(eventType);
	}

	/**
	 * @return the set of event types that this command handles
	 */
	public Set<Class<? extends ActionEvent>> getEventTypesToListenFor() {
		return Collections.unmodifiableSet(eventTypesToListenFor);
	}

	/*
	 * @Override public int hashCode() { return getClass().hashCode(); }
	 * @Override public boolean equals(Object o) { if ((o != null) &&
	 * getClass().equals(o.getClass())) { return true; } return false; }
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + super.hashCode();
	}
}
