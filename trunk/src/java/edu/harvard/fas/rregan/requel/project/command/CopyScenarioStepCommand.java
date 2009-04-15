/*
 * $Id: CopyScenarioStepCommand.java,v 1.1 2008/10/16 06:41:49 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.Step;

/**
 * Given a scenario step, create a new step copying references to other entites.
 * If a name isn't supplied a unique name is generated.
 * 
 * @author ron
 */
public interface CopyScenarioStepCommand extends EditCommand {

	/**
	 * @param step -
	 *            the Scenario Step to copy.
	 */
	public void setOriginalScenarioStep(Step step);

	/**
	 * @param newName -
	 *            the name for the new scenario step. if this is not set, or is
	 *            set to a name already in use, a unique name will be generated.
	 */
	public void setNewScenarioStepName(String newName);

	/**
	 * @return the new copy of the step.
	 */
	public Step getNewScenarioStep();
}
