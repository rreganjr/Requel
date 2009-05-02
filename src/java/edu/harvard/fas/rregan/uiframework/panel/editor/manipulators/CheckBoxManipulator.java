/*
 * $Id: CheckBoxManipulator.java,v 1.8 2008/10/11 21:47:44 rregan Exp $
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
