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
