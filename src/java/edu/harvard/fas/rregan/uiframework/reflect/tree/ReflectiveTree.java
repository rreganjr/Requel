/*
 * $Id: ReflectiveTree.java,v 1.1 2008/02/15 21:41:51 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.reflect.tree;

import org.apache.log4j.Logger;

import echopointng.Tree;
import echopointng.tree.TreeNode;
import echopointng.tree.TreeSelectionEvent;
import echopointng.tree.TreeSelectionListener;
import edu.harvard.fas.rregan.uiframework.reflect.ReflectUtils;
import edu.harvard.fas.rregan.uiframework.reflect.UIMethodDisplayHint;

public class ReflectiveTree extends Tree {
	static final long serialVersionUID = 0L;
	private static final Logger log = Logger.getLogger(ReflectiveTree.class);

	public ReflectiveTree(Object object) throws Exception {
		this(object, null, UIMethodDisplayHint.SHORT_OR_LONG);
	}

	public ReflectiveTree(Object object, String confineToPackagesStartingWith, int displayLevel)
			throws Exception {
		this(ReflectUtils.getLabelForObject(object), object, confineToPackagesStartingWith,
				displayLevel);
	}

	public ReflectiveTree(String nodeLabel, Object object, String confineToPackagesStartingWith,
			int displayLevel) throws Exception {
		super(new ReflectiveTreeModel(nodeLabel, object, confineToPackagesStartingWith,
				displayLevel));
		this.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent evt) {
				if (evt.getNewLeadSelectionPath() != null) {
					TreeNode node = (TreeNode) evt.getPath().getLastPathComponent();
					if (node instanceof ReflectiveTreeNode) {
						if (!((ReflectiveTreeNode) node).isInitialized()) {
							try {
								((ReflectiveTreeNode) node).initializeChildren();
							} catch (Exception e) {
								log.error("could not initialize node " + node + ": " + e, e);
							}
						}
					}
				}
			};
		});
	}
}
