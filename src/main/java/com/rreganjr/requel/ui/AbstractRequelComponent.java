/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.ui;

import net.sf.echopm.ResourceBundleHelper;
import com.rreganjr.requel.user.User;
import net.sf.echopm.panel.editor.AbstractComponent;
import net.sf.echopm.panel.editor.EditMode;

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
