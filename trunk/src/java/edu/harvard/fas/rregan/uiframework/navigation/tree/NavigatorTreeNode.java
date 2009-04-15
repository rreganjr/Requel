/*
 * $Id: NavigatorTreeNode.java,v 1.7 2008/09/12 00:15:12 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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