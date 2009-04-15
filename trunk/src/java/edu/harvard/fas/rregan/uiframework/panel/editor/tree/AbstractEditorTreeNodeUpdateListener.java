/*
 * $Id: AbstractEditorTreeNodeUpdateListener.java,v 1.1 2008/10/15 09:20:05 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor.tree;

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
public abstract class AbstractEditorTreeNodeUpdateListener implements
		EditorTreeNodeUpdateListener {
	static final long serialVersionUID = 0L;

	private final EditorTreeNode navigatorTreeNode;
	private final EditorTree tree;

	protected AbstractEditorTreeNodeUpdateListener(EditorTreeNode navigatorTreeNode,
			EditorTree tree) {
		super();
		this.navigatorTreeNode = navigatorTreeNode;
		this.tree = tree;
	}

	protected EditorTreeNode getNavigatorTreeNode() {
		return navigatorTreeNode;
	}

	protected EditorTree getTree() {
		return tree;
	}
}