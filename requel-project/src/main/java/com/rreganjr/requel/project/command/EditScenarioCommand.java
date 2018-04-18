/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.project.command;

import java.util.List;

import com.rreganjr.requel.project.Scenario;

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
