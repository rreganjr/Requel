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
import java.util.List;

import nextapp.echo2.app.table.DefaultTableColumnModel;
import nextapp.echo2.app.table.TableColumn;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.reflect.ReflectUtils;

public class ReflectiveTableColumnModel extends DefaultTableColumnModel {
	private static final Logger log = Logger.getLogger(ReflectiveTableColumnModel.class);
	private static final long serialVersionUID = 0;

	public ReflectiveTableColumnModel(Object object, int selectorLevel,
			String confineToPackagesStartingWith) {
		this(object.getClass(), selectorLevel, confineToPackagesStartingWith);
	}

	public ReflectiveTableColumnModel(Class<?> clazz, int selectorLevel,
			String confineToPackagesStartingWith) {
		List<Method> valueAccessors = ReflectUtils.getScalarPropertyMethods(clazz,
				confineToPackagesStartingWith, selectorLevel);
		int index = 0;
		for (Method valueAccessor : valueAccessors) {
			TableColumn column = new TableColumn(index);
			column.setHeaderValue(ReflectUtils.getLabelForMethod(valueAccessor));
			addColumn(column);
			index++;
		}
	}
}
