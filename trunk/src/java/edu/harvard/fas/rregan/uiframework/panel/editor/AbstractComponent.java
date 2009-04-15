/*
 * $Id: AbstractComponent.java,v 1.5 2008/10/12 07:55:32 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
