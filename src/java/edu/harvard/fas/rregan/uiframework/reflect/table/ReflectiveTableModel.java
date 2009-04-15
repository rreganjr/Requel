/*
 * $Id: ReflectiveTableModel.java,v 1.1 2008/02/15 21:40:32 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.reflect.table;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.reflect.ReflectUtils;

import nextapp.echo2.app.table.AbstractTableModel;


public class ReflectiveTableModel extends AbstractTableModel {
    private static final Logger log = Logger.getLogger(ReflectiveTableModel.class);
	private static final long serialVersionUID = 0;

	private List<Method> valueAccessors;
	private List<?> rowData;
	
	/**
	 * Create a ReflectiveTableModel
	 * @param clazz
	 */
	public ReflectiveTableModel(Class<?> clazz, String confineToPackagesStartingWith, int displayLevel) {
		super();
		valueAccessors = ReflectUtils.getScalarPropertyMethods(clazz, confineToPackagesStartingWith, displayLevel);		
	}

	public ReflectiveTableModel(Class<?> clazz, String confineToPackagesStartingWith, int displayLevel, Collection<?> objects) {
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
	 * 
     * @param column the column index (0-based)
     * @param row the row index (0-based)
	 */
	public Object getValueAt(int column, int row) {
		if (rowData != null) {
			if (column < 0 || column >= getColumnCount()) throw new ArrayIndexOutOfBoundsException("column " + column + " is out of the range 0.." + getColumnCount());
			if (row < 0 || row >= getRowCount()) throw new ArrayIndexOutOfBoundsException("row " + row + " is out of the range 0.." + getRowCount());

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
