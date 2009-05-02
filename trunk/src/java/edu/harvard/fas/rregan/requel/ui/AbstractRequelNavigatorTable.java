/*
 * $Id: AbstractRequelNavigatorTable.java,v 1.1 2009/01/08 06:48:46 rregan Exp $
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
package edu.harvard.fas.rregan.requel.ui;

import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.uiframework.panel.editor.AbstractComponent;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;

/**
 * Base class for Navigator tables that get embedded in editors that over rides
 * setEnabled() so that when an editor is in read-only mode the table sorting
 * and paging still works.<br>
 * This is because the EditorComponents addInput() disables the input component
 * if the editor isReadOnlyMode() method return true.
 * 
 * @author ron
 */
public class AbstractRequelNavigatorTable extends AbstractComponent {
	static final long serialVersionUID = 0L;

	protected AbstractRequelNavigatorTable(EditMode editMode) {
		super(editMode);
	}

	protected AbstractRequelNavigatorTable(EditMode editMode,
			ResourceBundleHelper resourceBundleHelper) {
		super(editMode, resourceBundleHelper);
	}

	@Override
	public void setEnabled(boolean newValue) {
		// the table is always enabled, this gets called with true when
		// the editor is read-only, but that disables the paging buttons.
		super.setEnabled(true);
	}
}
