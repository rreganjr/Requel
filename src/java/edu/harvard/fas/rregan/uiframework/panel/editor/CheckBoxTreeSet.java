/*
 * $Id: CheckBoxTreeSet.java,v 1.4 2009/02/20 09:32:31 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.panel.editor;

import java.util.Set;

import nextapp.echo2.app.Component;
import echopointng.Tree;
import echopointng.tree.DefaultMutableTreeNode;
import echopointng.tree.DefaultTreeCellRenderer;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.CheckBoxTreeSetManipulator;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulators;

/**
 * @author ron
 */
public class CheckBoxTreeSet extends Tree {
	static final long serialVersionUID = 0L;

	static {
		ComponentManipulators.setManipulator(CheckBoxTreeSet.class,
				new CheckBoxTreeSetManipulator());
	}

	/**
	 * 
	 */
	public CheckBoxTreeSet() {
		this(new CheckBoxTreeSetModel());
	}

	/**
	 * @param optionPaths
	 * @param initialSelection
	 */
	public CheckBoxTreeSet(Set<String> optionPaths, Set<String> initialSelection) {
		this(new CheckBoxTreeSetModel(optionPaths, initialSelection));
	}

	/**
	 * @param checkBoxModel
	 */
	public CheckBoxTreeSet(CheckBoxTreeSetModel checkBoxModel) {
		setModel(checkBoxModel);
		setCellRenderer(new PathLevelEnablementTreeCellRenderer());
	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
	}

	/**
	 * @param checkBoxModel
	 */
	public void setModel(CheckBoxTreeSetModel checkBoxModel) {
		super.setModel(checkBoxModel);
	}

	@Override
	public void setEnabled(boolean newValue) {
		if ((getModel() == null) || !((CheckBoxTreeSetModel) getModel()).isPathLevelEnablement()) {
			super.setEnabled(newValue);
		}
	}

	private static class PathLevelEnablementTreeCellRenderer extends DefaultTreeCellRenderer {
		static final long serialVersionUID = 0L;

		@Override
		public Component getTreeCellRendererComponent(Tree tree, Object node, boolean selected,
				boolean expanded, boolean leaf) {
			if ((tree instanceof CheckBoxTreeSet)
					&& (tree.getModel() instanceof CheckBoxTreeSetModel)) {
				CheckBoxTreeSetModel checkBoxTreeSetModel = (CheckBoxTreeSetModel) tree.getModel();
				if (node instanceof DefaultMutableTreeNode) {
					Object value = ((DefaultMutableTreeNode) node).getUserObject();
					if (value instanceof Component) {
						Component c = (Component) value;
						if (!checkBoxTreeSetModel.isPathLevelEnablement()) {
							c.setEnabled(tree.isRenderEnabled());
						}
						return c;
					}
				}
			}
			return null;
		}

	}
}
