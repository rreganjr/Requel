/*
 * $Id: DeleteScenarioCommand.java,v 1.1 2008/11/20 09:55:14 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.Scenario;

/**
 * @author ron
 */
public interface DeleteScenarioCommand extends EditCommand {

	/**
	 * Set the scenario to delete.
	 * 
	 * @param scenario
	 */
	public void setScenario(Scenario scenario);
}
