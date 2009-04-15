/*
 * $Id: AbstractComponentManipulator.java,v 1.5 2008/10/13 22:58:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor.manipulators;

import nextapp.echo2.app.Component;

/**
 * @author ron
 */
public abstract class AbstractComponentManipulator implements ComponentManipulator {

	public Object getOptionModel(Component component) {
		return null;
	}

	public void setOptionModel(Component component, Object optionModel) {
	}

	public Object getModel(Component component) {
		return null;
	}

	public void setModel(Component component, Object valueModel) {
	}
}
