/*
 * $Id: ScenarioType.java,v 1.3 2009/01/20 10:26:02 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

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
