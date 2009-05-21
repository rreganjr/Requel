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

package edu.harvard.fas.rregan.uiframework.panel.editor.manipulators;

import java.util.HashMap;
import java.util.Map;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.CheckBox;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.filetransfer.UploadSelect;
import nextapp.echo2.app.list.AbstractListComponent;
import nextapp.echo2.app.text.TextComponent;
import echopointng.ComboBox;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTable;

/**
 * @author ron
 */
public class ComponentManipulators {
	private static final Map<Class<? extends Component>, ComponentManipulator> componentManipulators = new HashMap<Class<? extends Component>, ComponentManipulator>();
	static {
		// add component manipulators for echo2 and echopointng components
		CollectionManipulator collectionManipulator = new CollectionManipulator();
		componentManipulators.put(TextComponent.class, new TextComponentManipulator());
		componentManipulators.put(Label.class, new LabelManipulator());
		componentManipulators.put(CheckBox.class, new CheckBoxManipulator());
		componentManipulators.put(AbstractListComponent.class,
				new AbstractListComponentManipulator());
		componentManipulators.put(ComboBox.class, new ComboBoxManipulator());
		componentManipulators.put(Row.class, collectionManipulator);
		componentManipulators.put(Column.class, collectionManipulator);
		componentManipulators.put(UploadSelect.class, new NullComponentManipulator());
		componentManipulators.put(Button.class, new NullComponentManipulator());
	}

	/**
	 * Each component should use this method in a static initializer block to
	 * add a manipulator for that component to the editor.
	 * 
	 * @param componentType
	 * @param manipulator
	 * @see NavigatorTable for an example of using this method in a static
	 *      initializer.
	 */
	public static void setManipulator(Class<? extends Component> componentType,
			ComponentManipulator manipulator) {
		componentManipulators.put(componentType, manipulator);
	}

	/**
	 * Get the component manipulator for a specific component. If a manipulator
	 * doesn't exist for the component's type, walk up the inheritance tree
	 * until a manipulator is found.
	 * 
	 * @param inputComponent
	 * @return
	 */
	public static ComponentManipulator getManipulator(Component inputComponent) {
		ComponentManipulator manipulator = null;
		Class<?> componentType = inputComponent.getClass();
		while (manipulator == null) {
			manipulator = componentManipulators.get(componentType);
			if (manipulator != null) {
				return manipulator;
			} else {
				componentType = componentType.getSuperclass();
				if (Object.class.equals(componentType)) {
					break;
				}
			}
		}
		throw new RuntimeException("no manipulator for component type " + inputComponent.getClass());
	}
}
