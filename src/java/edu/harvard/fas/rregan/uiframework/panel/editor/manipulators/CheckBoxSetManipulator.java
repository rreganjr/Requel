/*
 * $Id: CheckBoxSetManipulator.java,v 1.6 2008/10/13 22:58:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
