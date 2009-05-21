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
package edu.harvard.fas.rregan.uiframework.panel;

/**
 * @author ron
 */
public enum PanelActionType {

	/**
	 * Indicates the panel is an editor panel for editing some type of content.
	 */
	Editor(),
	/**
	 * Indicates the panel is a selector panel for choosing some type of
	 * content.
	 */
	Selector(),
	/**
	 * Indicates the panel is a navigator panel for listing and navigating over
	 * some type of content.
	 */
	Navigator(),
	/**
	 * Indicates the panel panel has an unspecified function and is only
	 * accessed by name.
	 */
	Unspecified();

	private PanelActionType() {
	}
}
