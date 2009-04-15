/*
 * $Id: DefaultEditorTreeNode.java,v 1.2 2008/10/22 22:41:56 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor.tree;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.event.ActionEvent;
import echopointng.tree.DefaultMutableTreeNode;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;

/**
 * A mutable tree node that contains an editor. The node may have a listener
 * that listens for update events for the entity object that the node is
 * displaying to indicate that the node view should be updated.
 * 
 * @author ron
 */
public class DefaultEditorTreeNode extends DefaultMutableTreeNode implements EditorTreeNode {
	static final long serialVersionUID = 0L;

	private final EventDispatcher eventDispatcher;
	private Component editor;
	private EditorTreeNodeUpdateListener updateListener;

	/**
	 * @param eventDispatcher
	 * @param editor
	 */
	public DefaultEditorTreeNode(EventDispatcher eventDispatcher, Component editor) {
		this(eventDispatcher, editor, editor);
	}

	/**
	 * @param eventDispatcher
	 * @param editor
	 * @param label
	 */
	public DefaultEditorTreeNode(EventDispatcher eventDispatcher, Component editor, Component label) {
		super(label);
		this.eventDispatcher = eventDispatcher;
		setEditor(editor);
	}

	public void actionPerformed(ActionEvent e) {
		if (updateListener != null) {
			updateListener.actionPerformed(e);
		}
	}

	public Component getEditor() {
		return editor;
	}

	public void setEditor(Component editor) {
		this.editor = editor;
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
	public void setUpdateListener(EditorTreeNodeUpdateListener updateListener) {
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