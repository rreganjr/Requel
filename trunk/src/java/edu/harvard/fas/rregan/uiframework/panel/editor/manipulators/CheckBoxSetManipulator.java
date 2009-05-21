/*
 * $Id$
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

import java.util.Collection;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.event.ChangeEvent;
import nextapp.echo2.app.event.ChangeListener;
import edu.harvard.fas.rregan.uiframework.panel.editor.CheckBoxSet;
import edu.harvard.fas.rregan.uiframework.panel.editor.CheckBoxSetModel;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;

/**
 * A generic interface for manipulating a CheckBoxSet control.
 * 
 * @author ron
 */
public class CheckBoxSetManipulator extends AbstractComponentManipulator {

	public <T> T getValue(Component component, Class<T> type) {
		return type.cast(getModel(component).getSelectedOptions());
	}

	public void setValue(Component component, Object value) {
		getModel(component).clearSelection();
		if (value instanceof Collection<?>) {
			for (Object o : (Collection<?>) value) {
				setSingleValue(component, o);
			}
		} else {
			setSingleValue(component, value);
		}
	}

	private void setSingleValue(Component component, Object value) {
		getModel(component).setSelected((String) value, true);
	}

	public void addListenerToDetectChangesToInput(final EditMode editMode, Component component) {
		getModel(component).addChangeListener(new ChangeListener() {
			static final long serialVersionUID = 0L;

			public void stateChanged(ChangeEvent e) {
				editMode.setStateEdited(true);
			}
		});
	}

	@Override
	public CheckBoxSetModel getModel(Component component) {
		return getComponent(component).getModel();
	}

	@Override
	public void setModel(Component component, Object valueModel) {
		getComponent(component).setModel((CheckBoxSetModel) valueModel);
	}

	private CheckBoxSet getComponent(Component component) {
		return (CheckBoxSet) component;
	}
}
