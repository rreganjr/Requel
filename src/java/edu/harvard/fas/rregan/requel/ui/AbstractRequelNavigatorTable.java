/*
 * $Id: AbstractRequelNavigatorTable.java,v 1.1 2009/01/08 06:48:46 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
