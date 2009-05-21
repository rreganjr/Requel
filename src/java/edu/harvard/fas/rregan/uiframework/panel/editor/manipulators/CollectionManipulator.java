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
