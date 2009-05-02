/*
 * $Id: ReflectiveTableSelector.java,v 1.1 2008/02/15 21:40:34 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.reflect.table;

import java.util.Collection;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;

/**
 * A table for selecting a single object from a set of objects. The columns of
 * the table are created by reflecting the supplied class to extract properties.
 * see
 * 
 * @ReflectiveTableModel and
 * @ReflectiveTableColumnModel for details of how the columns are created.
 * @author ron
 */
public class ReflectiveTableSelector extends ReflectiveTable implements ActionListener {
	private static final Logger log = Logger.getLogger(ReflectiveTableSelector.class);
	private static final long serialVersionUID = 0;

	private Object selectedOption;

	public ReflectiveTableSelector(Class<?> clazz, int selectorLevel,
			String confineToPackagesStartingWith) {
		this(clazz, selectorLevel, confineToPackagesStartingWith, null);
	}

	public ReflectiveTableSelector(Class<?> clazz, int selectorLevel,
			String confineToPackagesStartingWith, Collection<?> options) {
		super(clazz, selectorLevel, confineToPackagesStartingWith, options);
		addActionListener(this);
	}

	public Object getSelectedOption() {
		return this.selectedOption;
	}

	public void actionPerformed(ActionEvent arg0) {
		int selectedRow = getSelectionModel().getMinSelectedIndex();
		if (selectedRow > -1) {
			selectedOption = ((ReflectiveTableModel) getModel()).getRowData().get(selectedRow);
		}
	}
}
