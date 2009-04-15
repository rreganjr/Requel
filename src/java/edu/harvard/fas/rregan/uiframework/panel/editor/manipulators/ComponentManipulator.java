/*
 * $Id: ComponentManipulator.java,v 1.7 2008/10/11 21:47:44 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor.manipulators;

import nextapp.echo2.app.Component;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;

/**
 * A standard interface for working with input components to set and get the
 * model or value being edited.
 * 
 * @author ron
 */
public interface ComponentManipulator {

	public void addListenerToDetectChangesToInput(final EditMode editMode, Component component);

	public <T> T getValue(Component component, Class<T> type);

	public void setValue(Component component, Object value);

	public Object getModel(Component component);

	public void setModel(Component component, Object model);
}
