/*
 * $Id: ComboBoxManipulator.java,v 1.5 2008/10/13 22:58:58 rregan Exp $
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
import nextapp.echo2.app.event.DocumentEvent;
import nextapp.echo2.app.event.DocumentListener;
import nextapp.echo2.app.list.ListModel;
import nextapp.echo2.app.text.Document;
import echopointng.ComboBox;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;

/**
 * @author ron
 */
public class ComboBoxManipulator extends AbstractComponentManipulator {

	public <T> T getValue(Component component, Class<T> type) {
		return type.cast(getComponent(component).getText());
	}

	public void setValue(Component component, Object value) {
		getComponent(component).setText((String) value);
	}

	public void addListenerToDetectChangesToInput(final EditMode editMode, Component component) {
		final ComboBox comboBox = (ComboBox) component;
		comboBox.getTextField().getDocument().addDocumentListener(new DocumentListener() {
			static final long serialVersionUID = 0L;

			public void documentUpdate(DocumentEvent e) {
				editMode.setStateEdited(true);
			}
		});
	}

	@Override
	public Document getModel(Component component) {
		return getComponent(component).getTextField().getDocument();
	}

	@Override
	public void setModel(Component component, Object valueModel) {
		if (valueModel instanceof Document) {
			getComponent(component).getTextField().setDocument((Document) valueModel);
		}
		if (valueModel instanceof ListModel) {
			getComponent(component).setListModel((ListModel) valueModel);
		}
	}

	private ComboBox getComponent(Component component) {
		return (ComboBox) component;
	}
}
