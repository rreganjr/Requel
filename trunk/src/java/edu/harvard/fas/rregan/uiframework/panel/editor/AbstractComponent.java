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
package edu.harvard.fas.rregan.uiframework.panel.editor;

import java.util.Locale;

import nextapp.echo2.app.Column;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.uiframework.UIFrameworkApp;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * @author ron
 */
public abstract class AbstractComponent extends Column implements EditMode {
	static final long serialVersionUID = 0L;

	private ResourceBundleHelper resourceBundleHelper;
	private final EditMode editMode;

	protected AbstractComponent(EditMode editMode) {
		this.editMode = editMode;
	}

	protected AbstractComponent(EditMode editMode, ResourceBundleHelper resourceBundleHelper) {
		this(editMode);
		setResourceBundleHelper(resourceBundleHelper);
	}

	protected ResourceBundleHelper getResourceBundleHelper(Locale locale) {
		resourceBundleHelper.setLocale(locale);
		return resourceBundleHelper;
	}

	protected void setResourceBundleHelper(ResourceBundleHelper resourceBundleHelper) {
		this.resourceBundleHelper = resourceBundleHelper;
	}

	protected EditMode getEditMode() {
		return editMode;
	}

	/**
	 * This method should be overloaded to check permissions of the user making
	 * the edits and return true if the user only has read permissions.<br>
	 * use getApp().getUser() to get the user.
	 * 
	 * @return
	 */
	public boolean isReadOnlyMode() {
		return editMode.isReadOnlyMode();
	}

	@Override
	public boolean isStateEdited() {
		return editMode.isStateEdited();
	}

	@Override
	public void setStateEdited(boolean stateEdited) {
		editMode.setStateEdited(stateEdited);
	}

	/**
	 * @return
	 */
	public EventDispatcher getEventDispatcher() {
		return getApp().getEventDispatcher();
	}

	/**
	 * @return
	 */
	public UIFrameworkApp getApp() {
		return UIFrameworkApp.getApp();
	}
}
