/*
 * $Id: CollectionManipulator.java,v 1.10 2008/10/15 09:20:06 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor.manipulators;

import nextapp.echo2.app.Component;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;

/**
 * @author ron
 */
public class CollectionManipulator extends AbstractComponentManipulator {

	public void addListenerToDetectChangesToInput(EditMode editMode, Component container) {
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component component = container.getComponent(i);
			ComponentManipulator man = ComponentManipulators.getManipulator(component);
			if (man != null) {
				man.addListenerToDetectChangesToInput(editMode, component);
			}
		}
	}

	@Override
	public Object getModel(Component container) {
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component component = container.getComponent(i);
			ComponentManipulator man = ComponentManipulators.getManipulator(component);
			if (man != null) {
				return man.getModel(component);
			}
		}
		return null;
	}

	@Override
	public Object getOptionModel(Component container) {
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component component = container.getComponent(i);
			ComponentManipulator man = ComponentManipulators.getManipulator(component);
			if (man != null) {
				return man.getModel(component);
			}
		}
		return null;
	}

	@Override
	public void setModel(Component container, Object valueModel) {
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component component = container.getComponent(i);
			ComponentManipulator man = ComponentManipulators.getManipulator(component);
			if (man != null) {
				man.setModel(component, valueModel);
			}
		}
	}

	@Override
	public void setOptionModel(Component container, Object optionModel) {
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component component = container.getComponent(i);
			ComponentManipulator man = ComponentManipulators.getManipulator(component);
			if (man != null) {
				man.setModel(component, optionModel);
			}
		}
	}

	public <T> T getValue(Component container, Class<T> type) {
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component component = container.getComponent(i);
			ComponentManipulator man = ComponentManipulators.getManipulator(component);
			if (man != null) {
				return man.getValue(component, type);
			}
		}
		return null;
	}

	public void setValue(Component container, Object value) {
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component component = container.getComponent(i);
			ComponentManipulator man = ComponentManipulators.getManipulator(component);
			if (man != null) {
				man.setValue(component, value);
			}
		}
	}
}
