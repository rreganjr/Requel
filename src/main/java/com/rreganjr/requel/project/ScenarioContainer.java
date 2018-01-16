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

import java.util.Comparator;
import java.util.Set;

import com.rreganjr.requel.CreatedEntity;
import com.rreganjr.requel.Describable;

/**
 * A thing that can contain/refer to scenarios.
 * 
 * @author ron
 */
public interface ScenarioContainer extends Describable, CreatedEntity {
	/**
	 * The scenarios referenced.
	 * 
	 * @return
	 */
	public Set<Scenario> getScenarios();

	/**
	 * Compare the objects that contain Scenarios by the description.
	 */
	public static final Comparator<ScenarioContainer> COMPARATOR = new ScenarioContainerComparator();

	/**
	 * A Comparator for collections of Scenario containers.
	 */
	public static class ScenarioContainerComparator implements Comparator<ScenarioContainer> {
		@Override
		public int compare(ScenarioContainer o1, ScenarioContainer o2) {
			return o1.getDescription().compareTo(o2.getDescription());
		}
	}
}
