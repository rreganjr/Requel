/*
 * $Id: ReflectiveTableColumnModel.java,v 1.1 2008/02/15 21:40:35 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.reflect.table;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.reflect.ReflectUtils;

import nextapp.echo2.app.table.DefaultTableColumnModel;
import nextapp.echo2.app.table.TableColumn;


public class ReflectiveTableColumnModel extends DefaultTableColumnModel {
    private static final Logger log = Logger.getLogger(ReflectiveTableColumnModel.class);
	private static final long serialVersionUID = 0;

	public ReflectiveTableColumnModel(Object object, int selectorLevel, String confineToPackagesStartingWith) {
		this(object.getClass(), selectorLevel, confineToPackagesStartingWith);
	}

	public ReflectiveTableColumnModel(Class<?> clazz, int selectorLevel, String confineToPackagesStartingWith) {
		List<Method> valueAccessors = ReflectUtils.getScalarPropertyMethods(clazz, confineToPackagesStartingWith, selectorLevel);
		int index = 0;
		for (Method valueAccessor : valueAccessors) {
			TableColumn column = new TableColumn(index);
			column.setHeaderValue(ReflectUtils.getLabelForMethod(valueAccessor));
			addColumn(column);
			index++;
		}
	}
}
