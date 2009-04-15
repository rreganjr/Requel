/*
 * $Id: ComboBoxManipulator.java,v 1.5 2008/10/13 22:58:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
