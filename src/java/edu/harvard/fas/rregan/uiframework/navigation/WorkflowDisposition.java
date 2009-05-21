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
package edu.harvard.fas.rregan.uiframework.navigation;

/**
 * WorkflowDisposition indicates the relationship between panels for a set of
 * work. It is used by events and panel management
 * 
 * @author ron
 */
public enum WorkflowDisposition {

	/**
	 * If the disposition of an event is NewFlow then the PanelManager should
	 * cleanup the state of the previous workflow checking for unsaved data and
	 * allowing the user to save or throw away any changes.
	 */
	NewFlow(),

	/**
	 * If the disposition of an event is ContinueFlow, it indicates to the
	 * PanelManager that the state of the last window should be preservied while
	 * the user does additional work in the new panel. When the user is finished
	 * working in the new window, the previous window will be displayed.
	 */
	ContinueFlow();

	private WorkflowDisposition() {
	}
}
