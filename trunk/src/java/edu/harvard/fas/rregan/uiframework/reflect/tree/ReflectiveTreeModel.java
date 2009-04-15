/*
 * $Id: ReflectiveTreeModel.java,v 1.1 2008/02/15 21:41:51 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.reflect.tree;

import org.apache.log4j.Logger;

import echopointng.tree.DefaultTreeModel;

public class ReflectiveTreeModel extends DefaultTreeModel {
	private static final Logger log = Logger.getLogger(ReflectiveTreeModel.class);
	private static final long serialVersionUID = 0;

	public ReflectiveTreeModel(String nodeLabel, Object object,
			String confineToPackagesStartingWith, int displayLevel) throws Exception {
		super(
				new ReflectiveTreeNode(nodeLabel, object, confineToPackagesStartingWith,
						displayLevel));
	}
}
