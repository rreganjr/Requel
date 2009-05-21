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
package edu.harvard.fas.rregan.uiframework.panel.editor.tree;

import java.util.Map;
import java.util.WeakHashMap;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import echopointng.Tree;
import echopointng.tree.DefaultMutableTreeNode;
import echopointng.tree.TreeCellRenderer;
import echopointng.tree.TreeNode;
import echopointng.xhtml.XhtmlFragment;

/**
 * @author ron
 */
public class EditorTreeCellRenderer implements TreeCellRenderer {
	static final long serialVersionUID = 0L;

	private final Map<Object, Row> wrapperMap = new WeakHashMap<Object, Row>();

	/**
	 * @param enableDragAndDrop
	 */
	public EditorTreeCellRenderer() {
	}

	@Override
	public Component getTreeCellRendererComponent(Tree tree, Object node, boolean selected,
			boolean expanded, boolean leaf) {
		if (node instanceof EditorTreeNodeAbstractDecorator) {
			EditorTreeNodeAbstractDecorator decorator = (EditorTreeNodeAbstractDecorator) node;
			return decorator.getRenderComponent();
		}
		return getComponent((TreeNode) node);
	}

	@Override
	public Label getTreeCellRendererText(Tree tree, Object node, boolean sel, boolean expanded,
			boolean leaf) {
		return null;
	}

	@Override
	public XhtmlFragment getTreeCellRendererXhtml(Tree tree, Object value, boolean selected,
			boolean expanded, boolean leaf) {
		return null;
	}

	private Component getComponent(TreeNode node) {
		Object value;
		if (node instanceof EditorTreeNode) {
			value = ((EditorTreeNode) node).getEditor();
		} else if (node instanceof DefaultMutableTreeNode) {
			value = ((DefaultMutableTreeNode) node).getUserObject();
		} else {
			value = null;
		}
		if (value != null) {
			if (value instanceof Component) {
				return (Component) value;
			} else {
				return new Label(value.toString());
			}
		}
		return new Label(node.toString());
	}
}
