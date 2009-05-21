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
