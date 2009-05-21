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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;

import echopointng.tree.DefaultMutableTreeNode;
import edu.harvard.fas.rregan.uiframework.reflect.ReflectUtils;
import edu.harvard.fas.rregan.uiframework.reflect.UIMethodDisplayHint;

public class ReflectiveTreeNode extends DefaultMutableTreeNode {
	private static final Logger log = Logger.getLogger(ReflectiveTreeNode.class);
	private static final long serialVersionUID = 0;

	private String nodeLabel;
	private String confineToPackagesStartingWith;
	private int displayLevel;
	private boolean initialized = false;

	public ReflectiveTreeNode(String nodeLabel, Object target,
			String confineToPackagesStartingWith, int displayLevel) throws Exception {
		this(nodeLabel, target, confineToPackagesStartingWith, displayLevel, 2);
	}

	public ReflectiveTreeNode(String nodeLabel, Object target,
			String confineToPackagesStartingWith, int displayLevel, int levelsToInitialize)
			throws Exception {
		super(target, true);
		setNodeLabel(nodeLabel);
		setConfineToPackagesStartingWith(confineToPackagesStartingWith);
		setDisplayLevel(displayLevel);
		initializeChildren(levelsToInitialize);
	}

	protected String getNodeLabel() {
		return nodeLabel;
	}

	protected void setNodeLabel(String nodeLabel) {
		this.nodeLabel = nodeLabel;
	}

	protected String getConfineToPackagesStartingWith() {
		return confineToPackagesStartingWith;
	}

	protected void setConfineToPackagesStartingWith(String confineToPackagesStartingWith) {
		this.confineToPackagesStartingWith = confineToPackagesStartingWith;
	}

	protected int getDisplayLevel() {
		return displayLevel;
	}

	protected void setDisplayLevel(int displayLevel) {
		this.displayLevel = displayLevel;
	}

	protected boolean isInitialized() {
		return initialized;
	}

	protected void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	protected void initializeChildren() throws Exception {
		initializeChildren(1);
	}

	protected void initializeChildren(int levelsToInitialize) throws Exception {
		String nodeLabel = getNodeLabel();
		Object target = getUserObject();
		String confineToPackagesStartingWith = getConfineToPackagesStartingWith();
		int displayLevel = getDisplayLevel();

		if (levelsToInitialize > 0) {
			log.debug("initializing node for " + target);
			if (target.getClass().isArray()) {
				createArrayBasedNode(nodeLabel, (Object[]) target, confineToPackagesStartingWith,
						displayLevel, levelsToInitialize);
			} else if (Collection.class.isAssignableFrom(target.getClass())) {
				createCollectionBasedNode(nodeLabel, (Collection<?>) target,
						confineToPackagesStartingWith, displayLevel, levelsToInitialize);
			} else if (Map.class.isAssignableFrom(target.getClass())) {
				createMapBasedNode(nodeLabel, (Map<?, ?>) target, confineToPackagesStartingWith,
						displayLevel, levelsToInitialize);
			} else {
				for (Method method : ReflectUtils.getPropertyMethods(target.getClass(),
						confineToPackagesStartingWith, displayLevel)) {
					String propertyName = ReflectUtils.getLabelForMethod(method);
					UIMethodDisplayHint annotation = method
							.getAnnotation(UIMethodDisplayHint.class);
					if ((annotation != null) && (annotation.targetProperty() != null)
							&& (annotation.targetProperty().length() > 0)) {
						Object targetProperty = method.invoke(target);
						Method m = ReflectUtils.getPropertyMethod(targetProperty, annotation
								.targetProperty());
						Class<?> returnType = m.getReturnType();
						if (!ReflectUtils.isScalar(returnType)) {
							add(new ReflectiveTreeNode(propertyName, m.invoke(targetProperty),
									confineToPackagesStartingWith, displayLevel,
									(levelsToInitialize - 1)));
						}
					} else {
						Class<?> returnType = method.getReturnType();
						if (!ReflectUtils.isScalar(returnType)) {
							add(new ReflectiveTreeNode(propertyName, method.invoke(target),
									confineToPackagesStartingWith, displayLevel,
									(levelsToInitialize - 1)));
						}
					}
				}
			}
			setInitialized(true);
		} else if (log.isDebugEnabled()) {
			log.debug("not initializing node for " + target);
		}
	}

	private void createMapBasedNode(String nodeLabel, Map<?, ?> target,
			String confineToPackagesStartingWith, int displayLevel, int levelsToInitialize)
			throws Exception {
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(nodeLabel, true);
		for (Object key : target.keySet()) {
			Object val = target.get(key);
			String label = ReflectUtils.getLabelForObject(val);
			rootNode.add(new ReflectiveTreeNode(label, target.get(key),
					confineToPackagesStartingWith, displayLevel, (levelsToInitialize - 1)));
		}
		add(rootNode);
	}

	private void createCollectionBasedNode(String nodeLabel, Collection<?> target,
			String confineToPackagesStartingWith, int displayLevel, int levelsToInitialize)
			throws Exception {
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(nodeLabel, true);
		for (Object element : target) {
			String label = ReflectUtils.getLabelForObject(element);
			rootNode.add(new ReflectiveTreeNode(label, element, confineToPackagesStartingWith,
					displayLevel, (levelsToInitialize - 1)));
		}
		add(rootNode);
	}

	private void createArrayBasedNode(String nodeLabel, Object[] target,
			String confineToPackagesStartingWith, int displayLevel, int levelsToInitialize)
			throws Exception {
		createCollectionBasedNode(nodeLabel, Arrays.asList(target), confineToPackagesStartingWith,
				displayLevel, levelsToInitialize);
	}

	@Override
	public String toString() {
		return getNodeLabel();
	}
}
