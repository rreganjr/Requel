/*
 * $Id: Panel.java,v 1.13 2008/10/11 21:47:45 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.panel;

import nextapp.echo2.app.LayoutData;

/**
 * A Panel is a component with the responsability for handling a specific type
 * of activity on a specific type of content. For example Editing a User, or
 * selecting a user role type. A panel type may have an explicit name that is
 * used to refer directly to that window for cases when multiple windows may be
 * appropriate for a specific action and type. For example "simpleSelector" vs.
 * "advancedSelector" for selecting users. NOTE: this name is for all panels of
 * the same type and not an individual panel.
 * 
 * @author ron
 */
public interface Panel extends PanelDescriptor {

	/**
	 * The property to set in the resource bundle for the panel's title.
	 */
	public static final String PROP_PANEL_TITLE = "Panel.Title";

	/**
	 * The style name in the stylesheet for applying a style to the panel title.
	 */
	public static final String STYLE_NAME_PANEL_TITLE = "Panel.Title";

	/**
	 * The default style from the stylesheet.
	 */
	public static final String STYLE_NAME_DEFAULT = "Default";

	/**
	 * The style name to use for plain controls.
	 */
	public static final String STYLE_NAME_PLAIN = "Plain";

	/**
	 * The style to use for text that indicates a validation problem or error.
	 */
	public static final String STYLE_NAME_VALIDATION_LABEL = "ValidationLabel";

	/**
	 * 
	 */
	public static final String STYLE_NAME_HELP_LABEL = "HelpLabel";

	/**
	 * Set the name of the style to use for configuring this panel's style
	 * through the Echo2 stylesheet.
	 * 
	 * @param styleName
	 */
	public void setStyleName(String styleName);

	/**
	 * Set the layout data of this panel relative to its container. This is from
	 * the Echo2 Component class.
	 * 
	 * @param layoutData
	 */
	public void setLayoutData(LayoutData layoutData);

	/**
	 * @return
	 */
	public String getTitle();

	/**
	 * @return true if the panel's setup() method has been called since it was
	 *         created or last disposed.
	 */
	public boolean isInitialized();

	/**
	 * This method gets called once when the panel is first loaded so that it
	 * can create its UI components. The target object will be set before
	 * setup() is called.
	 */
	public void setup();

	/**
	 * This gets called before a panel is removed so that it can remove its
	 * components and cleanup any state.
	 */
	public void dispose();

	/**
	 * Get the content object this panel should act on.
	 * 
	 * @return
	 */
	public Object getTargetObject();

	/**
	 * Set the content object this panel should act on.
	 * 
	 * @param targetObject
	 */
	public void setTargetObject(Object targetObject);

	/**
	 * @return the expected destination object for events fired by this panel
	 *         for events that support a destination (like selecting an object.)
	 */
	public Object getDestinationObject();

	/**
	 * Set the destination of events that have a destination or expected
	 * recipient of an event.
	 * 
	 * @param destinationObject
	 */
	public void setDestinationObject(Object destinationObject);
}
