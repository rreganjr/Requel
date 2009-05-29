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
package edu.harvard.fas.rregan.uiframework.navigation;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.ImageReference;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * A NavigatorButton is a convience component for firing a predefined
 * ActionEvent when clicked. This is mainly for firing navigation events from a
 * navigator to start a workflow or from a panel to open up a helper panel.
 * 
 * @author ron
 */
public class NavigatorButton extends Button implements ActionListener {
	private static final Logger log = Logger.getLogger(NavigatorButton.class);
	static final long serialVersionUID = 0;

	private final EventDispatcher eventDispatcher;
	private ActionEvent eventToFire;

	/**
	 * Create a button with a text label, but without setting the event to fire.
	 * If the event to fire is not set, when the button is clicked nothing will
	 * happen.<br/>The event can be set through the setEventToFire() method.
	 * 
	 * @param label -
	 *            A string to use as the label of the button.
	 * @param eventDispatcher -
	 *            the dispatcher to fire the event through.
	 */
	public NavigatorButton(String label, EventDispatcher eventDispatcher) {
		this(label, eventDispatcher, null);
	}

	/**
	 * Create a button with a text label, and the event to fire. The event can
	 * be changed through the setEventToFire() method.
	 * 
	 * @param label -
	 *            A string to use as the label of the button.
	 * @param eventDispatcher -
	 *            the dispatcher to fire the event through.
	 * @param eventToFire -
	 *            the event to fire when the button is clicked
	 */
	public NavigatorButton(String label, EventDispatcher eventDispatcher, ActionEvent eventToFire) {
		super(label);
		this.eventDispatcher = eventDispatcher;
		this.eventToFire = eventToFire;
		addActionListener(this);
	}

	/**
	 * Create a button with an image label, but without setting the event to
	 * fire. If the event to fire is not set, when the button is clicked nothing
	 * will happen.<br/>The event can be set through the setEventToFire()
	 * method.
	 * 
	 * @param image -
	 *            An image reference to use as the label of the button.
	 * @param eventDispatcher -
	 *            the dispatcher to fire the event through.
	 */
	public NavigatorButton(ImageReference image, EventDispatcher eventDispatcher) {
		this(image, eventDispatcher, null);
	}

	/**
	 * Create a button with an image label, and the event to fire. The event can
	 * be changed through the setEventToFire() method.
	 * 
	 * @param image -
	 *            An image reference to use as the label of the button.
	 * @param eventDispatcher -
	 *            the dispatcher to fire the event through.
	 * @param eventToFire -
	 *            the event to fire when the button is clicked
	 */
	public NavigatorButton(ImageReference image, EventDispatcher eventDispatcher,
			ActionEvent eventToFire) {
		super(image);
		this.eventDispatcher = eventDispatcher;
		this.eventToFire = eventToFire;
		addActionListener(this);
	}

	/**
	 * Set the event to fire when the button is clicked.
	 * 
	 * @param eventToFire
	 */
	public void setEventToFire(ActionEvent eventToFire) {
		this.eventToFire = eventToFire;
	}

	public void actionPerformed(ActionEvent e) {
		if (eventToFire == null) {
			log.warn(this + " does not have an eventToFire set.");
		} else {
			eventDispatcher.dispatchEvent(eventToFire);
		}
	}
}
