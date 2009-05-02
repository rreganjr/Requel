/*
 * $Id: AbstractNavigatorTreeNodeUpdateListener.java,v 1.4 2008/03/07 10:37:27 rregan Exp $
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

/**
 * a NavigatorTreeNodeUpdateListener listens for update events for the object
 * that the node is displaying, updates the nodes data object and notifies the
 * tree to refresh the view when it changes. This abstract version manages the
 * related objects and listening for events, it should be extended with logic
 * for determining when the tree display is changed by changes to the underlying
 * object and for updating the data stored in the node.
 * 
 * @author ron
 */
public abstract class AbstractNavigatorTreeNodeUpdateListener implements
		NavigatorTreeNodeUpdateListener {
	static final long serialVersionUID = 0L;

	private final NavigatorTreeNode navigatorTreeNode;
	private final NavigatorTree tree;

	protected AbstractNavigatorTreeNodeUpdateListener(NavigatorTreeNode navigatorTreeNode,
			NavigatorTree tree) {
		super();
		this.navigatorTreeNode = navigatorTreeNode;
		this.tree = tree;
	}

	protected NavigatorTreeNode getNavigatorTreeNode() {
		return navigatorTreeNode;
	}

	protected NavigatorTree getTree() {
		return tree;
	}
}