/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
 */
package com.rreganjr.requel.project;

/**
 * The type/disposition of a scenario or step, such as a primary, alternative,
 * or exceptional case.
 * 
 * @author ron
 */
public enum ScenarioType {

	/**
	 * Indicate that this is a precursor to the scenario. If a precondition
	 * isn't meet the scenario cannot begin.
	 */
	PreCondition,

	/**
	 * A successful scenario or step that is the primary expected case.
	 */
	Primary,

	/**
	 * An optional step in a scenario.
	 */
	Optional,

	/**
	 * A successful scenario or step that is an alternative to the primary
	 * expected case.
	 */
	Alternative,

	/**
	 * A scenario or step indicating a failed interaction.
	 */
	Exception;

	private ScenarioType() {
	}
}
