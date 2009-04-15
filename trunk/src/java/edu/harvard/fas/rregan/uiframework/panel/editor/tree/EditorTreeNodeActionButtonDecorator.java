/*
 * $Id: EditorTreeNodeActionButtonDecorator.java,v 1.4 2009/01/22 10:36:30 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.panel.editor.tree;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.layout.RowLayoutData;
import echopointng.tree.MutableTreeNode;

/**
 * A decorator for adding a button to a tree node that invokes some action.
 * 
 * @author ron
 */
public class EditorTreeNodeActionButtonDecorator extends EditorTreeNodeAbstractDecorator implements
		EditorTreeNode {
	static final long serialVersionUID = 0L;

	private final Button button;
	private final RowLayoutData rowLayoutTop = new RowLayoutData();

	/**
	 * @param treeNode
	 * @param tree
	 * @param button
	 */
	public EditorTreeNodeActionButtonDecorator(MutableTreeNode treeNode, EditorTree tree,
			Button button) {
		super(treeNode, tree);
		this.button = button;
		rowLayoutTop.setAlignment(Alignment.ALIGN_TOP);
	}

	@Override
	public void dispose() {
		super.dispose();
		button.dispose();
	}

	@Override
	public Component getRenderComponent() {
		Component editor = super.getRenderComponent();
		Row wrapper = new Row();
		// button.setLayoutData(rowLayoutTop);
		// editor.setLayoutData(rowLayoutTop);
		// TODO: add a side/alignment property to indicate which side the button
		// goes on.
		wrapper.add(button);
		wrapper.add(editor);
		return wrapper;
	}
}
