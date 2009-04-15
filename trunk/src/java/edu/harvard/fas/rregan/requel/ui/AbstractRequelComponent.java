/*
 * $Id: AbstractRequelComponent.java,v 1.4 2008/10/11 21:47:44 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui;

import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.uiframework.panel.editor.AbstractComponent;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;

/**
 * @author ron
 */
public class AbstractRequelComponent extends AbstractComponent {
	static final long serialVersionUID = 0L;

	protected AbstractRequelComponent(EditMode editMode) {
		super(editMode);
	}

	protected AbstractRequelComponent(EditMode editMode, ResourceBundleHelper resourceBundleHelper) {
		super(editMode, resourceBundleHelper);
	}

	protected User getCurrentUser() {
		return (User) getApp().getUser();
	}
}
