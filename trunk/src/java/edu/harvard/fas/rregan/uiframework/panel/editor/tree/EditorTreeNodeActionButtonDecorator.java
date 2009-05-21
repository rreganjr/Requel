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
