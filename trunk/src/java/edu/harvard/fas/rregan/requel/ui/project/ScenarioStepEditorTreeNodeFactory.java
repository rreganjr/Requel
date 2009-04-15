/*
 * $Id: ScenarioStepEditorTreeNodeFactory.java,v 1.8 2009/01/21 09:23:18 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.project;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;

import echopointng.tree.MutableTreeNode;
import edu.harvard.fas.rregan.requel.project.Step;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.panel.editor.tree.AbstractEditorTreeNodeFactory;
import edu.harvard.fas.rregan.uiframework.panel.editor.tree.DefaultEditorTreeNode;
import edu.harvard.fas.rregan.uiframework.panel.editor.tree.EditorTree;
import edu.harvard.fas.rregan.uiframework.panel.editor.tree.EditorTreeNode;
import edu.harvard.fas.rregan.uiframework.panel.editor.tree.EditorTreeNodeFactory;

/**
 * A factory for created EditorTree nodes for editing scenario steps. It creates
 * a ScenarioStepEditor and decorates it with a button for adding sub-nodes
 * below this node and for removing the node from its parent.
 * 
 * @author ron
 */
public class ScenarioStepEditorTreeNodeFactory extends AbstractEditorTreeNodeFactory {
	private static final Logger log = Logger.getLogger(ScenarioStepEditorTreeNodeFactory.class);

	/**
	 * The style name for the add child node button.
	 */
	public static final String STYLE_NAME_ADD_CHILD_NODE_BUTTON = "ScenarioStepEditor.AddChildNodeButton";

	/**
	 * The style name for the remove node button.
	 */
	public static final String STYLE_NAME_REMOVE_NODE_BUTTON = "ScenarioStepEditor.RemoveNodeButton";

	/**
	 * 
	 */
	public ScenarioStepEditorTreeNodeFactory() {
		this(ScenarioStepEditorTreeNodeFactory.class.getName(), Step.class);
	}

	/**
	 * @param resourceBundleName
	 * @param targetClass
	 */
	protected ScenarioStepEditorTreeNodeFactory(String resourceBundleName, Class<?> targetClass) {
		super(resourceBundleName, targetClass);
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.editor.tree.EditorTreeNodeFactory#createTreeNode(edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher,
	 *      edu.harvard.fas.rregan.uiframework.panel.editor.tree.EditorTree,
	 *      java.lang.Object)
	 */
	@Override
	public MutableTreeNode createTreeNode(EventDispatcher eventDispatcher, EditorTree tree,
			Object object) {
		Step step = (Step) object;
		ScenarioStepEditor stepEditor = new ScenarioStepEditor(tree.getEditMode(),
				getResourceBundleHelper(tree.getLocale()), step);
		EditorTreeNode stepNode = new DefaultEditorTreeNode(eventDispatcher, stepEditor);

		// add a button that adds child nodes
		stepNode = addEditorTreeNodeActionButtonDecorator(tree, stepNode, "",
				STYLE_NAME_ADD_CHILD_NODE_BUTTON, new AddChildActionListener(tree, stepNode,
						Step.class));

		// add a button that removes this node
		stepNode = addEditorTreeNodeActionButtonDecorator(tree, stepNode, "",
				STYLE_NAME_REMOVE_NODE_BUTTON, new RemoveNodeActionListener(tree, stepNode));

		return stepNode;
	}

	/**
	 * A listener for adding new tree nodes.
	 * 
	 * @author ron
	 */
	public static class AddChildActionListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final EditorTree editorTree;
		private final EditorTreeNode treeNode;
		private final Class<?> childTargetType;

		protected AddChildActionListener(EditorTree tree, EditorTreeNode treeNode,
				Class<?> childTargetType) {
			this.editorTree = tree;
			this.treeNode = treeNode;
			this.childTargetType = childTargetType;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			EditorTreeNodeFactory factory = editorTree.getEditorTreeNodeFactory(childTargetType);
			MutableTreeNode newNode = factory.createTreeNode(treeNode.getEventDispatcher(),
					editorTree, null);
			editorTree.getModel().insertNodeInto(newNode, treeNode, treeNode.getChildCount());
			// TODO: maybe the decorators cause this not to work?
			// editorTree.expandPath(new
			// TreePath(editorTree.getModel().getPathToRoot(treeNode)));
			editorTree.expandAll();
		}
	}

	/**
	 * A listener for removing a node from the tree.
	 * 
	 * @author ron
	 */
	public static class RemoveNodeActionListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final EditorTree editorTree;
		private final EditorTreeNode treeNode;

		protected RemoveNodeActionListener(EditorTree tree, EditorTreeNode treeNode) {
			this.editorTree = tree;
			this.treeNode = treeNode;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				editorTree.getModel().removeNodeFromParent(treeNode);
				// TODO: maybe the decorators cause this not to work?
				// editorTree.expandPath(new
				// TreePath(editorTree.getModel().getPathToRoot(treeNode)));
				editorTree.expandAll();
			} catch (Exception ex) {
				log.error("Exception removing tree node: " + treeNode, ex);
			}
		}
	}
}
