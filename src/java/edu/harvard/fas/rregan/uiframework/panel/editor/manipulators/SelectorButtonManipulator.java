/*
 * $Id: SelectorButtonManipulator.java,v 1.8 2008/10/13 22:58:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor.manipulators;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.event.ChangeEvent;
import nextapp.echo2.app.event.ChangeListener;
import edu.harvard.fas.rregan.uiframework.navigation.SelectorButton;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;

/**
 * A generic interface for manipulating a SelectorButton control.
 * 
 * @author ron
 */
public class SelectorButtonManipulator extends AbstractComponentManipulator {

	public void addListenerToDetectChangesToInput(final EditMode editMode, Component component) {
		getComponent(component).addChangeListener(new ChangeListener() {
			static final long serialVersionUID = 0L;

			public void stateChanged(ChangeEvent e) {
				editMode.setStateEdited(true);
			}
		});
	}

	public <T> T getValue(Component component, Class<T> type) {
		return type.cast(getComponent(component).getSelectedObject());
	}

	public void setValue(Component component, Object value) {
		getComponent(component).setSelectedObject(value);
	}

	private SelectorButton getComponent(Component component) {
		return (SelectorButton) component;
	}

}
