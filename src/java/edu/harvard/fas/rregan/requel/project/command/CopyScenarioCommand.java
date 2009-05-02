/*
 * $Id: CopyScenarioCommand.java,v 1.2 2008/10/16 06:41:50 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
 * Elicitation System.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
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
