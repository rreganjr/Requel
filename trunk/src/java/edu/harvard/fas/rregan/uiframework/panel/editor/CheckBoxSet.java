/*
 * $Id: CheckBoxSet.java,v 1.5 2008/10/11 08:22:31 rregan Exp $
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
import java.util.Map;
import java.util.Set;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.CheckBox;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.layout.RowLayoutData;
import edu.harvard.fas.rregan.uiframework.panel.Panel;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.CheckBoxSetManipulator;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulators;

/**
 * TODO: make the checkboxes layout in a grid such that the options are
 * displayed in as close to a square configuration as possible.
 * 
 * @author ron
 */
public class CheckBoxSet extends Row {
	static final long serialVersionUID = 0L;

	static {
		ComponentManipulators.setManipulator(CheckBoxSet.class, new CheckBoxSetManipulator());
	}

	private final Map<String, CheckBox> optionBoxes = new HashMap<String, CheckBox>();
	private final RowLayoutData firstCellLayout;
	private final RowLayoutData restCellLayout;

	private CheckBoxSetModel checkBoxModel;

	/**
	 * 
	 */
	public CheckBoxSet() {
		this(new CheckBoxSetModel());

	}

	/**
	 * @param checkBoxModel
	 */
	public CheckBoxSet(CheckBoxSetModel checkBoxModel) {
		firstCellLayout = new RowLayoutData();
		firstCellLayout.setAlignment(new Alignment(Alignment.CENTER, Alignment.CENTER));
		firstCellLayout.setInsets(new Insets(0, 0, 5, 0));

		restCellLayout = new RowLayoutData();
		restCellLayout.setAlignment(new Alignment(Alignment.CENTER, Alignment.CENTER));
		restCellLayout.setInsets(new Insets(5, 0, 5, 0));

		setModel(checkBoxModel);
		setStyleName(Panel.STYLE_NAME_DEFAULT);
	}

	/**
	 * @param options
	 * @param initialSelection
	 * @param singleSelection
	 */
	public CheckBoxSet(Set<String> options, Set<String> initialSelection, boolean singleSelection) {
		this(new CheckBoxSetModel(options, initialSelection, singleSelection));
	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
		optionBoxes.clear();
	}

	/**
	 * @return
	 */
	public CheckBoxSetModel getModel() {
		return checkBoxModel;
	}

	/**
	 * @param checkBoxModel
	 */
	public void setModel(CheckBoxSetModel checkBoxModel) {
		this.checkBoxModel = checkBoxModel;
		optionBoxes.clear();

		// the layout for the first cell doesn't have an inset on the left
		RowLayoutData cellLayout = firstCellLayout;
		for (String option : checkBoxModel.getOptions()) {
			CheckBox optionBox = new CheckBox(option);
			optionBox.setStyleName(Panel.STYLE_NAME_DEFAULT);
			optionBox.setModel(checkBoxModel.getToggleButtonModel(option));
			optionBoxes.put(option, optionBox);
			optionBox.setLayoutData(cellLayout);

			// set the layout for the next cell
			cellLayout = restCellLayout;

			add(optionBox);
		}
	}
}
