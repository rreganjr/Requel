/*
 * $Id: EditScenarioCommand.java,v 1.4 2008/11/06 06:19:49 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import java.util.List;

import edu.harvard.fas.rregan.requel.project.Scenario;

/**
 * @author ron
 */
public interface EditScenarioCommand extends EditScenarioStepCommand {

	/**
	 * Set the Scenario to edit.
	 * 
	 * @param Scenario
	 */
	public void setScenario(Scenario Scenario);

	/**
	 * Get the new or updated Scenario.
	 * 
	 * @return
	 */
	public Scenario getScenario();

	/**
	 * @param editStepCommands -
	 *            a list of step or scenario edit commands corresponding to the
	 *            steps of this scenario to execute and then add the steps
	 */
	public void setStepCommands(List<EditScenarioStepCommand> editStepCommands);

}
