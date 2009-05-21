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
 * Describes the action, content type, and/or name of a panel.
 * 
 * @author ron
 */
public interface PanelDescriptor {

	/**
	 * @return the type of content the panel supports.
	 */
	public Class<?> getSupportedContentType();

	/**
	 * @return the type of action the panel supports.
	 */
	public PanelActionType getSupportedActionType();

	/**
	 * @return for a named panel return the name, otherwise return null.
	 */
	public String getPanelName();

	/**
	 * @return the class of the panel being described.
	 */
	public Class<? extends Panel> getPanelType();

	/**
	 * tell the descriptor to cleanup its resources and ui components.
	 */
	public void dispose();
}
