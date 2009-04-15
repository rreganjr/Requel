/*
 * $Id: CheckBoxManipulator.java,v 1.8 2008/10/11 21:47:44 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor.manipulators;

import nextapp.echo2.app.CheckBox;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.button.ToggleButtonModel;
import nextapp.echo2.app.event.ChangeEvent;
import nextapp.echo2.app.event.ChangeListener;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;

/**
 * A generic interface for manipulating a CheckBox control.
 * 
 * @author ron
 */
public class CheckBoxManipulator extends AbstractComponentManipulator {

	public <T> T getValue(Component component, Class<T> type) {
		return type.cast(getComponent(component).isSelected());
	}

	public void setValue(Component component, Object value) {
		if ((value != null) && (value instanceof Boolean)) {
			getComponent(component).setSelected((Boolean) value);
		} else {
			getComponent(component).setSelected(false);
		}
	}

	public void addListenerToDetectChangesToInput(final EditMode editMode, Component component) {
		getComponent(component).addChangeListener(new ChangeListener() {
			static final long serialVersionUID = 0L;

			public void stateChanged(ChangeEvent e) {
				editMode.setStateEdited(true);
			}
		});
	}

	@Override
	public ToggleButtonModel getModel(Component component) {
		return (ToggleButtonModel) getComponent(component).getModel();
	}

	@Override
	public void setModel(Component component, Object valueModel) {
		getComponent(component).setModel((ToggleButtonModel) valueModel);
	}

	private CheckBox getComponent(Component component) {
		return (CheckBox) component;
	}
}
