/*
 * $Id: EditorTreeManipulator.java,v 1.2 2008/10/16 01:00:30 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor.tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nextapp.echo2.app.Component;
import echopointng.tree.DefaultMutableTreeNode;
import echopointng.tree.DefaultTreeModel;
import echopointng.tree.TreeModelEvent;
import echopointng.tree.TreeModelListener;
import echopointng.tree.TreeNode;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.AbstractComponentManipulator;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulator;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulators;

/**
 * @author ron
 */
public class EditorTreeManipulator extends AbstractComponentManipulator {

	public <T> T getValue(Component component, Class<T> type) {
		if (Map.class.equals(type)) {
			Map<List<Object>, Object> values = new TreeMap<List<Object>, Object>(new NodeComparator());
			DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) getModel(component)
					.getRoot();
			if (rootNode != null) {
				Enumeration<DefaultMutableTreeNode> enm = rootNode.depthFirstEnumeration();
				while (enm.hasMoreElements()) {
					DefaultMutableTreeNode node = enm.nextElement();
					if (node instanceof EditorTreeNode) {
						EditorTreeNode editorNode = (EditorTreeNode) node;
						Component editor = editorNode.getEditor();
						ComponentManipulator man = ComponentManipulators.getManipulator(editor);
						if (man != null) {
							values.put(getPathEntites(editorNode), man.getValue(editor,
									Object.class));
						}
					}
				}
			}
			return type.cast(values);
		}
		return type.cast(getModel(component).getRoot());
	}

	private List<Object> getPathEntites(EditorTreeNode editorNode) {
		List<Object> path = new ArrayList<Object>(editorNode.getDepth() + 1);
		for (TreeNode ancestorNode : editorNode.getPath()) {
			if (ancestorNode instanceof EditorTreeNode) {
				Component editor = ((EditorTreeNode)ancestorNode).getEditor();
				ComponentManipulator man = ComponentManipulators.getManipulator(editor);
				if (man != null) {
					path.add(man.getValue(editor, Object.class));
				}
			}
		}
		return path;
	}

	public void setValue(Component component, Object value) {
		getComponent(component).setRootObject(value);
	}

	public void addListenerToDetectChangesToInput(final EditMode editMode, Component component) {
		getModel(component).addTreeModelListener(new TreeModelListener() {
			static final long serialVersionUID = 0L;

			@Override
			public void treeNodesChanged(TreeModelEvent e) {
				editMode.setStateEdited(true);
			}

			@Override
			public void treeNodesInserted(TreeModelEvent e) {
				editMode.setStateEdited(true);
			}

			@Override
			public void treeNodesRemoved(TreeModelEvent e) {
				editMode.setStateEdited(true);
			}

			@Override
			public void treeStructureChanged(TreeModelEvent e) {
				editMode.setStateEdited(true);
			}

		});
	}

	@Override
	public DefaultTreeModel getModel(Component component) {
		return getComponent(component).getModel();
	}

	@Override
	public void setModel(Component component, Object valueModel) {
		getComponent(component).setModel((DefaultTreeModel) valueModel);
	}

	private EditorTree getComponent(Component component) {
		return (EditorTree) component;
	}
	
	private static class NodeComparator implements Comparator<List<Object>> {

		@Override
		public int compare(List<Object> o1, List<Object> o2) {
			if (o1.size() != o2.size()) {
				return (o1.size() - o2.size());
			}
 
			for (int index = 0; index < o1.size(); index++) {
				int hash = (o1.get(index).hashCode() - o2.get(index).hashCode());
				if (hash != 0) {
					return hash;
				}
			}
			return 0;
		}
		
	}
}
