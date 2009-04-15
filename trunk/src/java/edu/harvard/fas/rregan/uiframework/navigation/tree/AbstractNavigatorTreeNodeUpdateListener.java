/*
 * $Id: AbstractNavigatorTreeNodeUpdateListener.java,v 1.4 2008/03/07 10:37:27 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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