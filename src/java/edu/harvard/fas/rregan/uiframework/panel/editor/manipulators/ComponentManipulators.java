/*
 * $Id: ComponentManipulators.java,v 1.2 2008/10/15 09:20:06 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
