/*
 * $Id: NullComponentManipulator.java,v 1.2 2008/10/11 21:47:44 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor.manipulators;

import nextapp.echo2.app.Component;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;

/**
 * @author ron
 */
public class NullComponentManipulator extends AbstractComponentManipulator {

	@Override
	public void addListenerToDetectChangesToInput(EditMode editMode, Component component) {
	}

	@Override
	public Object getModel(Component component) {
		return null;
	}

	@Override
	public <T> T getValue(Component component, Class<T> type) {
		return null;
	}

	@Override
	public void setModel(Component component, Object model) {
	}

	@Override
	public void setValue(Component component, Object value) {
	}

}
