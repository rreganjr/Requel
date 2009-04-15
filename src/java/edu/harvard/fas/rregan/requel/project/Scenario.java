/*
 * $Id: Scenario.java,v 1.5 2009/02/11 12:49:32 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
