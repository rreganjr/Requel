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
 */
package edu.harvard.fas.rregan.requel.project;

import java.util.List;
import java.util.Set;

/**
 * A scenario defines a sequence of steps to complete a use case. Each step may
 * contain alternate or exceptional sub-steps.
 * 
 * @author ron
 */
public interface Scenario extends Step {

	/**
	 * @return An ordered list of steps, each step is represented by a scenario
	 *         that may contain alternate or exception sub-steps.
	 */
	public List<Step> getSteps();

	/**
	 * @return The use cases that have this scenario as the primary.
	 */
	public Set<UseCase> getUsingUseCases();

	/**
	 * This function searches through all reachable scenarios at all levels
	 * below this scenario.
	 * 
	 * @param step -
	 *            a step or scenario to test.
	 * @return true if the supplied step is a step in this scenario or in any
	 *         scenario it uses.
	 */
	public boolean usesStep(Step step);
}
