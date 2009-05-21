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
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTable;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableModel;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;

/**
 * @author ron
 */
public class NavigatorTableManipulator extends AbstractComponentManipulator {

	public <T> T getValue(Component component, Class<T> type) {
		return type.cast(getModel(component).getEntities());
	}

	public void setValue(Component component, Object value) {
		getComponent(component).setModel(new NavigatorTableModel((Collection) value));
	}

	public void addListenerToDetectChangesToInput(final EditMode editMode, Component component) {
		// A navigator table doesn't require
	}

	@Override
	public NavigatorTableModel getModel(Component component) {
		return getComponent(component).getModel();
	}

	@Override
	public void setModel(Component component, Object valueModel) {
		getComponent(component).setModel((NavigatorTableModel) valueModel);
	}

	private NavigatorTable getComponent(Component component) {
		return (NavigatorTable) component;
	}
}
