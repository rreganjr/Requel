/*
 * $Id: AbstractListComponentManipulator.java,v 1.10 2008/10/13 22:58:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor.manipulators;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.event.ChangeEvent;
import nextapp.echo2.app.event.ChangeListener;
import nextapp.echo2.app.list.AbstractListComponent;
import nextapp.echo2.app.list.DefaultListSelectionModel;
import nextapp.echo2.app.list.ListModel;
import nextapp.echo2.app.list.ListSelectionModel;
import edu.harvard.fas.rregan.uiframework.panel.editor.CombinedListModel;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;

/**
 * @author ron
 */
public class AbstractListComponentManipulator extends AbstractComponentManipulator {

	public <T> T getValue(Component component, Class<T> type) {
		if (getModel(component).getSelectionMode() == DefaultListSelectionModel.MULTIPLE_SELECTION) {
			Collection<Object> selections = createAppropriateCollection((Class) type);
			for (int i = 0; i < getModel(component).size(); i++) {
				if (getModel(component).isSelectedIndex(i)) {
					selections.add(getModel(component).get(i));
				}
			}
			return type.cast(selections);
		} else {
			return type.cast(getModel(component).get(getModel(component).getMaxSelectedIndex()));
		}
	}

	private <T extends Collection<Object>> T createAppropriateCollection(Class<T> type) {
		if (Set.class.isAssignableFrom(type)) {
			return type.cast(new HashSet<Object>());
		} else if (List.class.isAssignableFrom(type)) {
			return type.cast(new HashSet<Object>());
		}
		return null;
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
		int index = getModel(component).indexOf(value);
		if (index > -1) {
			getModel(component).setSelectedIndex(index, true);
		}
	}

	public void addListenerToDetectChangesToInput(final EditMode editMode, Component component) {
		getComponent(component).getSelectionModel().addChangeListener(new ChangeListener() {
			static final long serialVersionUID = 0L;

			public void stateChanged(ChangeEvent e) {
				editMode.setStateEdited(true);
			}
		});
	}

	@Override
	public CombinedListModel getModel(Component component) {
		return (CombinedListModel) getComponent(component).getSelectionModel();
	}

	@Override
	public void setModel(Component component, Object valueModel) {
		getComponent(component).setSelectionModel((ListSelectionModel) valueModel);
		getComponent(component).setModel((ListModel) valueModel);
	}

	private AbstractListComponent getComponent(Component component) {
		return (AbstractListComponent) component;
	}
}
