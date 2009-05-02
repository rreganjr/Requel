/*
 * $Id: EditMode.java,v 1.3 2009/01/21 09:23:21 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.panel.editor;

/**
 * Interface to describe something that can have a read-only mode and edit
 * state. This is typically implemented by a component or panel and supplied to
 * sub-components to determine if they should support editing or read-only mode
 * and to indicate to the parent if a data value has been changed.
 * 
 * @author ron
 */
public interface EditMode {

	/**
	 * A component will use this during setup to configure an editor for editing
	 * or for view only. It is expected that this value won't change after a
	 * panel or component is already configured.
	 * 
	 * @return true if the mode only allows reading.
	 */
	public boolean isReadOnlyMode();

	/**
	 * This is used by a parent component to determine if a sub-component data
	 * value has changed and a save is need.
	 * 
	 * @return true if the state has changed.
	 */
	public boolean isStateEdited();

	/**
	 * An editor component that is a sub-component of a panel or compound editor
	 * calls this method to indicate to the parent that a data value has changed
	 * and needs to be validated and saved.
	 * 
	 * @param stateEdited
	 */
	public void setStateEdited(boolean stateEdited);
}
