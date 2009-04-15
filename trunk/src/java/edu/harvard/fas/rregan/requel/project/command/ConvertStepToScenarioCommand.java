/*
 * $Id: ConvertStepToScenarioCommand.java,v 1.1 2008/10/16 06:41:49 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.project.Step;

/**
 * Given a scenario step, convert the step into a scenario updating all the
 * scenarios the reference the step.
 * 
 * @author ron
 */
public interface ConvertStepToScenarioCommand extends EditScenarioCommand {

	/**
	 * @param step -
	 *            the Scenario Step to copy.
	 */
	public void setOriginalScenarioStep(Step step);
}
