/*
 * $Id: EditorTreeNodeDragAndDropDecorator.java,v 1.8 2009/01/23 09:54:25 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
