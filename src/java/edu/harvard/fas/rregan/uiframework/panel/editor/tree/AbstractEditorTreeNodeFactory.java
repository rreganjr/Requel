/*
 * $Id: AbstractEditorTreeNodeFactory.java,v 1.3 2009/01/21 09:23:21 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.panel.editor.tree;

import java.util.Locale;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.event.ActionListener;
import edu.harvard.fas.rregan.ResourceBundleHelper;

/**
 * A base factory for creating NavigatorTreeNode nodes for specific types of
 * entity objects. This class manages and makes available the type of entity
 * object for which the factory produces nodes.
 * 
 * @author ron
 */
public abstract class AbstractEditorTreeNodeFactory implements EditorTreeNodeFactory {

	private final ResourceBundleHelper resourceBundleHelper;
	private Class<?> targetClass;

	protected AbstractEditorTreeNodeFactory(String resourceBundleName, Class<?> targetClass) {
		resourceBundleHelper = new ResourceBundleHelper(resourceBundleName);
		setTargetClass(targetClass);
	}

	/**
	 * Return the type of object this factory builds tree nodes for.
	 * 
	 * @return
	 */
	public Class<?> getTargetType() {
		return targetClass;
	}

	protected void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	protected ResourceBundleHelper getResourceBundleHelper(Locale locale) {
		resourceBundleHelper.setLocale(locale);
		return resourceBundleHelper;
	}

	protected EditorTreeNode addEditorTreeNodeActionButtonDecorator(EditorTree tree,
			EditorTreeNode decoratedNode, String labelText, String styleName,
			ActionListener actionListener) {
		Button button = new Button(labelText);
		button.setStyleName(styleName);
		button.addActionListener(actionListener);
		return new EditorTreeNodeActionButtonDecorator(decoratedNode, tree, button);
	}
}
