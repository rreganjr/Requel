/*
 * $Id: LabelManipulator.java,v 1.8 2008/10/11 21:47:44 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor.manipulators;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.Label;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;

/**
 * @author ron
 */
public class LabelManipulator extends AbstractComponentManipulator {

	public <T> T getValue(Component component, Class<T> type) {
		final Label text = (Label) component;
		return type.cast(text.getText());
	}

	public void setValue(Component component, Object value) {
		final Label text = (Label) component;
		text.setText((String) value);
	}

	public void addListenerToDetectChangesToInput(final EditMode editMode, Component component) {
		// label's don't get changed by users
	}

	@Override
	public String getModel(Component component) {
		return getValue(component, String.class);
	}

	@Override
	public void setModel(Component component, Object valueModel) {
		setValue(component, valueModel);
	}

}
