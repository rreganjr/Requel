/*
 * $Id: EditorTreeNodeDragAndDropDecorator.java,v 1.8 2009/01/23 09:54:25 rregan Exp $
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

import nextapp.echo2.app.Column;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Row;
import nextapp.echo2.extras.app.DragSource;
import echopointng.tree.MutableTreeNode;

/**
 * A decorator for adding dragging and dropping between tree nodes in the
 * editor.
 * 
 * @author ron
 */
public class EditorTreeNodeDragAndDropDecorator extends EditorTreeNodeAbstractDecorator implements
		EditorTreeNode {
	static final long serialVersionUID = 0L;

	private final Component dropTarget;
	private final DragSource dragSource;

	/**
	 * @param treeNode
	 * @param tree
	 */
	public EditorTreeNodeDragAndDropDecorator(MutableTreeNode treeNode, EditorTree tree) {
		super(treeNode, tree);
		this.dragSource = tree.getDragSource(this);
		this.dropTarget = tree.getDropTarget(this);
	}

	/**
	 * @return The target component for dropping a drag source for this tree
	 *         node.
	 */
	public Component getDropTarget() {
		return dropTarget;
	}

	/**
	 * @return The draggable component for dropping on a target represting this
	 *         tree node.
	 */
	public DragSource getDragSource() {
		return dragSource;
	}

	@Override
	public void dispose() {
		super.dispose();
		dragSource.dispose();
		dropTarget.dispose();
	}

	@Override
	public Component getRenderComponent() {
		Component editor = super.getRenderComponent();
		Row wrapper = new Row();
		// arrange the drag and drop components vertically
		// to the left of the editor.
		Column vertWrapper = new Column();
		vertWrapper.setCellSpacing(new Extent(0));
		vertWrapper.add(getDragSource());
		vertWrapper.add(getDropTarget());
		wrapper.add(vertWrapper);
		wrapper.add(editor);
		return wrapper;
	}
}
