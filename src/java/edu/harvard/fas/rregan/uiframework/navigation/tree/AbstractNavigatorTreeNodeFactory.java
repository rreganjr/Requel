/*
 * $Id: AbstractNavigatorTreeNodeFactory.java,v 1.5 2008/05/06 09:15:41 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
