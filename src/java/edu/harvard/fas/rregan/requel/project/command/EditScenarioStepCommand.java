/*
 * $Id: EditScenarioStepCommand.java,v 1.3 2009/01/27 09:30:16 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.project.Step;

/**
 * @author ron
 */
public interface EditScenarioStepCommand extends EditTextEntityCommand {

	/**
	 * Set the Scenario Step to edit.
	 * 
	 * @param step
	 */
	public void setStep(Step step);

	/**
	 * Get the new or updated Scenario Step.
	 * 
	 * @return
	 */
	public Step getStep();

	/**
	 * @param scenarioTypeName -
	 *            the name of the type of Scenario Step.
	 */
	public void setScenarioTypeName(String scenarioTypeName);

}
