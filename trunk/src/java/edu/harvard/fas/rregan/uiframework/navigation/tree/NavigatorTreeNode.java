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
package edu.harvard.fas.rregan.uiframework.navigation.tree;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import echopointng.tree.DefaultMutableTreeNode;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;

/**
 * A mutable tree node that fires a navigation event when clicked. The node may
 * have a listener that that listens for update events for the entity object
 * that the node is displaying to indicate that the node view should be updated.
 * 
 * @author ron
 */
public class NavigatorTreeNode extends DefaultMutableTreeNode implements ActionListener {
	static final long serialVersionUID = 0L;

	private final EventDispatcher eventDispatcher;
	private Object targetObject;
	private ActionEvent eventToFire;
	private NavigatorTreeNodeUpdateListener updateListener;

	public NavigatorTreeNode(EventDispatcher eventDispatcher, Object targetObject) {
		this(eventDispatcher, targetObject, targetObject);
	}

	public NavigatorTreeNode(EventDispatcher eventDispatcher, Object targetObject, Object label) {
		this(eventDispatcher, targetObject, label, null);
	}

	public NavigatorTreeNode(EventDispatcher eventDispatcher, Object targetObject, Object label,
			ActionEvent eventToFire) {
		super(label);
		this.eventDispatcher = eventDispatcher;
		setEventToFire(eventToFire);
		setTargetObject(targetObject);
	}

	public ActionEvent getEventToFire() {
		return eventToFire;
	}

	public void setEventToFire(ActionEvent eventToFire) {
		this.eventToFire = eventToFire;
	}

	public void actionPerformed(ActionEvent e) {
		if (updateListener != null) {
			updateListener.actionPerformed(e);
		}
	}

	public Object getTargetObject() {
		return targetObject;
	}

	public void setTargetObject(Object targetObject) {
		this.targetObject = targetObject;
	}

	public EventDispatcher getEventDispatcher() {
		return eventDispatcher;
	}

	/**
	 * Set a listener that gets notified if the object for the node is modified.
	 * The new listener will be registered with the event dispatcher by this
	 * method. If another listener is already set, it will be unregistered
	 * before the new one is registered. If null is supplied, the old listener
	 * will be unregistered and no listeners will be listening for this node.
	 * 
	 * @param updateListener
	 */
	public void setUpdateListener(NavigatorTreeNodeUpdateListener updateListener) {
		if (this.updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					this.updateListener);
		}
		this.updateListener = updateListener;
		if (this.updateListener != null) {
			getEventDispatcher().addEventTypeActionListener(UpdateEntityEvent.class,
					this.updateListener);
		}
	}

	public void dispose() {
		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
			this.updateListener = null;
		}
	}
}