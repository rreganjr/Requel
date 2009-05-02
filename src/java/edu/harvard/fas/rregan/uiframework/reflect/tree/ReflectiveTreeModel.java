/*
 * $Id: ReflectiveTreeModel.java,v 1.1 2008/02/15 21:41:51 rregan Exp $
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
