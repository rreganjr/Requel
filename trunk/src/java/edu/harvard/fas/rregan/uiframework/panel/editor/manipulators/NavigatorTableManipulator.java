/*
 * $Id: NavigatorTableManipulator.java,v 1.4 2008/10/13 22:58:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
