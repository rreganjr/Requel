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
 *
 */
package com.rreganjr.requel.project;

import java.util.Set;

/**
 * A Step in a Scenario, which may be a Scenario itself.
 * 
 * @author ron
 */
public interface Step extends TextEntity, Comparable<Step> {

	/**
	 * @return The type/disposition of a scenario or step, such as a primary,
	 *         alternative, or exceptional case.
	 */
	public ScenarioType getType();

	/**
	 * @param scenarioType -
	 *            the new type
	 */
	public void setType(ScenarioType scenarioType);

	/**
	 * @return The set of scenarios that have this scenario as a step.
	 */
	public Set<Scenario> getUsingScenarios();

}
