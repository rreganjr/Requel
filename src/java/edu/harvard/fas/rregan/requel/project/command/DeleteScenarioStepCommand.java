/*
 * $Id: DeleteScenarioStepCommand.java,v 1.1 2008/11/20 09:55:15 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.Step;

/**
 * @author ron
 */
public interface DeleteScenarioStepCommand extends EditCommand {

	/**
	 * Set the scenarioStep to delete.
	 * 
	 * @param scenarioStep
	 */
	public void setScenarioStep(Step scenarioStep);
}
