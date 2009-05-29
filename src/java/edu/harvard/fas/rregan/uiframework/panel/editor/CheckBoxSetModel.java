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
package edu.harvard.fas.rregan.uiframework.panel.editor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nextapp.echo2.app.button.DefaultToggleButtonModel;
import nextapp.echo2.app.button.ToggleButtonModel;
import nextapp.echo2.app.event.ChangeEvent;
import nextapp.echo2.app.event.ChangeListener;

/**
 * @author ron
 */
public class CheckBoxSetModel implements ChangeListener {
	static final long serialVersionUID = 0L;

	private final Set<ChangeListener> changeListeners = new HashSet<ChangeListener>();
	private final Map<String, ToggleButtonModel> checkBoxModels = new HashMap<String, ToggleButtonModel>();
	private boolean singleSelection = false;

	/**
	 * 
	 */
	public CheckBoxSetModel() {
	}

	/**
	 * @param options
	 * @param initialSelection
	 * @param singleSelection
	 */
	public CheckBoxSetModel(Set<String> options, Set<String> initialSelection,
			boolean singleSelection) {
		setSingleSelection(singleSelection);
		for (String option : options) {
			ToggleButtonModel optionModel = new DefaultToggleButtonModel();
			optionModel.setSelected(initialSelection.contains(option));
			putToggleButtonModel(option, optionModel);
		}
	}

	/**
	 * Add a ToggleButtonModel that holds the state for the specified check
	 * option.
	 * 
	 * @param optionName
	 * @param model
	 */
	public void putToggleButtonModel(String optionName, ToggleButtonModel model) {
		model.addChangeListener(this);
		checkBoxModels.put(optionName, model);
	}

	/**
	 * Return the underlying ToggleButtonModel for a specific option.
	 * 
	 * @param optionName
	 * @return
	 */
	public ToggleButtonModel getToggleButtonModel(String optionName) {
		return checkBoxModels.get(optionName);
	}

	/**
	 * Configure the model to support single or multiple selection.
	 * 
	 * @param singleSelection -
	 *            set true if only one checkbox can be checked at one time
	 */
	public void setSingleSelection(boolean singleSelection) {
		this.singleSelection = singleSelection;
	}

	/**
	 * @return true if only one checkbox can be checked at one time.
	 */
	public boolean isSingleSelection() {
		return singleSelection;
	}

	public boolean isSelected(String option) {
		if (checkBoxModels.containsKey(option)) {
			return checkBoxModels.get(option).isSelected();
		}
		return false;
	}

	public void setSelected(String option, boolean selected) {
		if (checkBoxModels.containsKey(option)) {
			checkBoxModels.get(option).setSelected(selected);
		}
	}

	public void clearSelection() {
		for (String optionName : checkBoxModels.keySet()) {
			if (checkBoxModels.get(optionName).isSelected()) {
				checkBoxModels.get(optionName).setSelected(false);
			}
		}
	}

	/**
	 * Return the set of options
	 * 
	 * @return
	 */
	public Set<String> getOptions() {
		Set<String> options = new HashSet<String>();
		for (String optionName : checkBoxModels.keySet()) {
			options.add(optionName);
		}
		return options;
	}

	/**
	 * Return the set of selected options by name.
	 * 
	 * @return
	 */
	public Set<String> getSelectedOptions() {
		Set<String> selectedOptions = new HashSet<String>();
		for (String optionName : checkBoxModels.keySet()) {
			if (checkBoxModels.get(optionName).isSelected()) {
				selectedOptions.add(optionName);
			}
		}
		return selectedOptions;
	}

	public void stateChanged(ChangeEvent e) {
		if (isSingleSelection()) {
			ToggleButtonModel source = (ToggleButtonModel) e.getSource();
			if (source.isSelected()) {
				for (ToggleButtonModel model : checkBoxModels.values()) {
					if (!model.equals(source)) {
						model.setSelected(false);
					}
				}
			}
		}
	}

	public void addChangeListener(ChangeListener l) {
		changeListeners.add(l);
	}

	public void removeChangeListener(ChangeListener l) {
		changeListeners.remove(l);
	}

	/**
	 * Notifies all listeners that have registered for this event type.
	 */
	public void fireStateChanged() {
		if (!changeListeners.isEmpty()) {
			ChangeEvent e = new ChangeEvent(this);
			for (ChangeListener listener : changeListeners) {
				listener.stateChanged(e);
			}
		}
	}

}
