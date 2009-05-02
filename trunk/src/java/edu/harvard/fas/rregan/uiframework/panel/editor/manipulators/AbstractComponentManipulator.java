/*
 * $Id: AbstractComponentManipulator.java,v 1.5 2008/10/13 22:58:58 rregan Exp $
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

import nextapp.echo2.app.Component;

/**
 * @author ron
 */
public abstract class AbstractComponentManipulator implements ComponentManipulator {

	public Object getOptionModel(Component component) {
		return null;
	}

	public void setOptionModel(Component component, Object optionModel) {
	}

	public Object getModel(Component component) {
		return null;
	}

	public void setModel(Component component, Object valueModel) {
	}
}
