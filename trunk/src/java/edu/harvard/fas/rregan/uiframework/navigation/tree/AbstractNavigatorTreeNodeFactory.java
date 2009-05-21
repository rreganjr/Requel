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
package edu.harvard.fas.rregan.uiframework.navigation.tree;

import java.util.Locale;

import edu.harvard.fas.rregan.ResourceBundleHelper;

/**
 * A base factory for creating NavigatorTreeNode nodes for specific types of
 * entity objects. This class manages and makes available the type of entity
 * object that the factory produces nodes for.
 * 
 * @author ron
 */
public abstract class AbstractNavigatorTreeNodeFactory implements NavigatorTreeNodeFactory {

	private final ResourceBundleHelper resourceBundleHelper;
	private Class<?> targetClass;

	protected AbstractNavigatorTreeNodeFactory(String resourceBundleName, Class<?> targetClass) {
		resourceBundleHelper = new ResourceBundleHelper(resourceBundleName);
		setTargetClass(targetClass);
	}

	/**
	 * Return the type of object this factory builds tree nodes for.
	 * 
	 * @return
	 */
	public Class<?> getTargetClass() {
		return targetClass;
	}

	protected void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	protected ResourceBundleHelper getResourceBundleHelper(Locale locale) {
		resourceBundleHelper.setLocale(locale);
		return resourceBundleHelper;
	}
}
