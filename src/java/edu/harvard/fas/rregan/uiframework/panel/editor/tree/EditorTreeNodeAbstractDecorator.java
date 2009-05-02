/*
 * $Id: EditorTreeNodeAbstractDecorator.java,v 1.3 2009/01/15 01:33:44 rregan Exp $
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

import java.util.ArrayList;
import java.util.Enumeration;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.event.ActionEvent;
import echopointng.tree.DefaultMutableTreeNode;
import echopointng.tree.MutableTreeNode;
import echopointng.tree.TreeNode;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * @author ron
 */
public abstract class EditorTreeNodeAbstractDecorator implements EditorTreeNode {
	static final long serialVersionUID = 0L;
	private final MutableTreeNode decoratedNode;

	/**
	 * @param treeNode
	 * @param tree
	 */
	public EditorTreeNodeAbstractDecorator(MutableTreeNode treeNode, EditorTree tree) {
		this.decoratedNode = treeNode;
	}

	@Override
	public void insert(MutableTreeNode child, int index) {
		this.children();
		decoratedNode.insert(child, index);
	}

	@Override
	public void remove(int index) {
		decoratedNode.remove(index);
	}

	@Override
	public void remove(MutableTreeNode node) {
		decoratedNode.remove(node);
	}

	@Override
	public void removeFromParent() {
		MutableTreeNode parent = (MutableTreeNode) getParent();
		if (parent != null) {
			parent.remove(this);
		}
	}

	@Override
	public void setParent(MutableTreeNode newParent) {
		decoratedNode.setParent(newParent);
	}

	// this should be on the MutableTreeNode interface, but its only on the
	// implementation class DefaultMutableTreeNode
	public Object getUserObject() {
		if (decoratedNode instanceof DefaultMutableTreeNode) {
			return ((DefaultMutableTreeNode) decoratedNode).getUserObject();
		}
		throw new RuntimeException("stupid tree implementation.");
	}

	@Override
	public void setUserObject(Object object) {
		decoratedNode.setUserObject(object);

	}

	@Override
	public Enumeration children() {
		return decoratedNode.children();
	}

	@Override
	public String getActionCommand() {
		return decoratedNode.getActionCommand();
	}

	@Override
	public boolean getAllowsChildren() {
		return decoratedNode.getAllowsChildren();
	}

	@Override
	public TreeNode getChildAt(int childIndex) {
		return decoratedNode.getChildAt(childIndex);
	}

	@Override
	public int getChildCount() {
		return decoratedNode.getChildCount();
	}

	@Override
	public int getIndex(TreeNode node) {
		return decoratedNode.getIndex(node);
	}

	@Override
	public TreeNode getParent() {
		return decoratedNode.getParent();
	}

	@Override
	public boolean isLeaf() {
		return decoratedNode.isLeaf();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (decoratedNode instanceof EditorTreeNode) {
			((EditorTreeNode) decoratedNode).actionPerformed(e);
		}

	}

	@Override
	public void dispose() {
		if (decoratedNode instanceof EditorTreeNode) {
			((EditorTreeNode) decoratedNode).dispose();
		}
	}

	@Override
	public int getDepth() {
		if (decoratedNode instanceof EditorTreeNode) {
			return ((EditorTreeNode) decoratedNode).getDepth();
		} else if (decoratedNode instanceof DefaultMutableTreeNode) {
			return ((DefaultMutableTreeNode) decoratedNode).getDepth();
		} else if (decoratedNode.getChildCount() == 0) {
			return 0;
		} else {
			throw new RuntimeException("getDepth isn't implemented for general TreeNode.");
		}
	}

	@Override
	public Component getEditor() {
		if (decoratedNode instanceof EditorTreeNode) {
			return ((EditorTreeNode) decoratedNode).getEditor();
		}
		return null;
	}

	@Override
	public EventDispatcher getEventDispatcher() {
		if (decoratedNode instanceof EditorTreeNode) {
			return ((EditorTreeNode) decoratedNode).getEventDispatcher();
		}
		return null;
	}

	@Override
	public TreeNode[] getPath() {
		if (decoratedNode instanceof EditorTreeNode) {
			return ((EditorTreeNode) decoratedNode).getPath();
		} else if (decoratedNode instanceof DefaultMutableTreeNode) {
			return ((DefaultMutableTreeNode) decoratedNode).getPath();
		} else {
			ArrayList<TreeNode> nodeList = new ArrayList<TreeNode>(10);
			TreeNode parent = decoratedNode;
			while (parent != null) {
				nodeList.add(parent);
				parent = parent.getParent();
			}
			ArrayList<TreeNode> reverseNodes = new ArrayList<TreeNode>(nodeList.size());
			for (int i = nodeList.size() - 1; i >= 0; i--) {
				reverseNodes.add(nodeList.get(i));
			}
			return reverseNodes.toArray(new TreeNode[nodeList.size()]);
		}
	}

	@Override
	public void add(MutableTreeNode newChild) {
		if (decoratedNode instanceof EditorTreeNode) {
			((EditorTreeNode) decoratedNode).add(newChild);
		} else if (decoratedNode instanceof DefaultMutableTreeNode) {
			((DefaultMutableTreeNode) decoratedNode).add(newChild);
		} else {
			throw new RuntimeException("The decorated node " + decoratedNode
					+ " doesn't support add()");
		}
	}

	@Override
	public void setEditor(Component editor) {
		if (decoratedNode instanceof EditorTreeNode) {
			((EditorTreeNode) decoratedNode).setEditor(editor);
		}
	}

	@Override
	public void setUpdateListener(EditorTreeNodeUpdateListener updateListener) {
		if (decoratedNode instanceof EditorTreeNode) {
			((EditorTreeNode) decoratedNode).setUpdateListener(updateListener);
		}
	}

	// the tree object model depends on the implementation and not the
	// interfaces so the decorated node must be accessed and passed to
	// tree methods.
	public MutableTreeNode getDecoratedNode() {
		return decoratedNode;
	}

	/**
	 * @return The node with decorations
	 */
	public Component getRenderComponent() {
		if (decoratedNode instanceof EditorTreeNodeAbstractDecorator) {
			return ((EditorTreeNodeAbstractDecorator) decoratedNode).getRenderComponent();
		}
		return getEditor();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		// get the root decorated node of this and the obj supplied and compare
		// them:
		while (obj instanceof EditorTreeNodeAbstractDecorator) {
			obj = ((EditorTreeNodeAbstractDecorator) obj).getDecoratedNode();
		}
		Object thisNode = getDecoratedNode();
		while (thisNode instanceof EditorTreeNodeAbstractDecorator) {
			thisNode = ((EditorTreeNodeAbstractDecorator) thisNode).getDecoratedNode();
		}
		return thisNode.equals(obj);
	}

	@Override
	public int hashCode() {
		return decoratedNode.hashCode();
	}
}
