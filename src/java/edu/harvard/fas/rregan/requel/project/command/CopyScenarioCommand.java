/*
 * $Id: CopyScenarioCommand.java,v 1.2 2008/10/16 06:41:50 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.Scenario;

/**
 * Given a scenario, create a new scenario copying references to other entites.
 * If a name isn't supplied a unique name is generated.
 * 
 * @author ron
 */
public interface CopyScenarioCommand extends EditCommand {

	/**
	 * @param scenario -
	 *            the Scenario to copy.
	 */
	public void setOriginalScenario(Scenario scenario);

	/**
	 * @param newName -
	 *            the name for the new scenario. if this is not set, or is set
	 *            to a name already in use, a unique name will be generated.
	 */
	public void setNewScenarioName(String newName);

	/**
	 * @return the new copy of the scenario.
	 */
	public Scenario getNewScenario();
}
