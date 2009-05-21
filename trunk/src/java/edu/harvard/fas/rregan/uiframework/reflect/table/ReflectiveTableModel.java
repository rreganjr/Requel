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
package edu.harvard.fas.rregan.uiframework.reflect.table;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nextapp.echo2.app.table.AbstractTableModel;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.reflect.ReflectUtils;

public class ReflectiveTableModel extends AbstractTableModel {
	private static final Logger log = Logger.getLogger(ReflectiveTableModel.class);
	private static final long serialVersionUID = 0;

	private final List<Method> valueAccessors;
	private List<?> rowData;

	/**
	 * Create a ReflectiveTableModel
	 * 
	 * @param clazz
	 */
	public ReflectiveTableModel(Class<?> clazz, String confineToPackagesStartingWith,
			int displayLevel) {
		super();
		valueAccessors = ReflectUtils.getScalarPropertyMethods(clazz,
				confineToPackagesStartingWith, displayLevel);
	}

	public ReflectiveTableModel(Class<?> clazz, String confineToPackagesStartingWith,
			int displayLevel, Collection<?> objects) {
		this(clazz, confineToPackagesStartingWith, displayLevel);
		setRowData(objects);
	}

	public void setRowData(Collection<?> objects) {
		if (objects != null) {
			rowData = new ArrayList<Object>(objects);
		} else {
			rowData = new ArrayList<Object>();
		}
	}

	public List<?> getRowData() {
		return rowData;
	}

	public int getColumnCount() {
		return valueAccessors.size();
	}

	public int getRowCount() {
		if (rowData != null) {
			return rowData.size();
		}
		return 0;
	}

	/**
	 * @param column
	 *            the column index (0-based)
	 * @param row
	 *            the row index (0-based)
	 */
	public Object getValueAt(int column, int row) {
		if (rowData != null) {
			if ((column < 0) || (column >= getColumnCount())) {
				throw new ArrayIndexOutOfBoundsException("column " + column
						+ " is out of the range 0.." + getColumnCount());
			}
			if ((row < 0) || (row >= getRowCount())) {
				throw new ArrayIndexOutOfBoundsException("row " + row + " is out of the range 0.."
						+ getRowCount());
			}

			try {
				Object rowObject = rowData.get(row);
				Method m = valueAccessors.get(column);
				return m.invoke(rowObject);
			} catch (Exception e) {
				log.warn("Could not get value at col " + column + " row " + row + ": " + e, e);
			}
		}
		return null;
	}
}
