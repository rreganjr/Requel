/*
 * $Id: EditorTreeNode.java,v 1.4 2008/10/28 07:13:24 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.panel.editor.tree;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.event.ActionListener;
import echopointng.tree.MutableTreeNode;
import echopointng.tree.TreeNode;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * A node in an editor tree that contains an editor component for editing the
 * content of the node.
 * 
 * @author ron
 */
public interface EditorTreeNode extends MutableTreeNode, ActionListener {

	/**
	 * @return The editor component of this node.
	 */
	public Component getEditor();

	public void setEditor(Component editor);

	public EventDispatcher getEventDispatcher();

	/**
	 * Set a listener that gets notified if the object for the node is modified.
	 * The new listener will be registered with the event dispatcher by this
	 * method. If another listener is already set, it will be unregistered
	 * before the new one is registered. If null is supplied, the old listener
	 * will be unregistered and no listeners will be listening for this node.
	 * 
	 * @param updateListener
	 */
	public void setUpdateListener(EditorTreeNodeUpdateListener updateListener);

	public void dispose();

	// copied from DefaultMutableTreeeNode because they aren't in
	// MutableTreeNode but should be

	public int getDepth();

	public TreeNode[] getPath();

	public void add(MutableTreeNode newChild);
}