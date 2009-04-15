/*
 * $Id: Step.java,v 1.1 2008/10/09 22:55:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

import java.util.Set;

import edu.harvard.fas.rregan.requel.project.TextEntity;

/**
 * A Step in a Scenario, which may be a Scenario itself.
 * 
 * @author ron
 */
public interface Step extends TextEntity, Comparable<Step> {

	/**
	 * @return The type/disposition of a scenario or step, such as a primary, 
	 * alternative, or exceptional case.
	 */
	public ScenarioType getType();
	
	/**
	 * 
	 * @param scenarioType - the new type
	 */
	public void setType(ScenarioType scenarioType);

	/**
	 * 
	 * @return The set of scenarios that have this scenario as a step.
	 */
	public Set<Scenario> getUsingScenarios();

}
