/*
 * $Id: ToggleButtonModelEx.java,v 1.1 2008/03/05 10:52:13 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.panel.editor;

import nextapp.echo2.app.button.DefaultToggleButtonModel;

/**
 * @author ron
 */
public class ToggleButtonModelEx extends DefaultToggleButtonModel {
	static final long serialVersionUID = 0;

	/**
	 * @param selected
	 */
	public ToggleButtonModelEx(boolean selected) {
		super();
		setSelected(selected);
	}
}
