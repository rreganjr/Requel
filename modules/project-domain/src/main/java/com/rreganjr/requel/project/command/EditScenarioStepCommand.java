/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import com.rreganjr.requel.project.Step;

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


	/**
	 * #468: sets who does the step: PRIMARY_ACTOR, SYSTEM, NAMED (with {@link #setActorName}), or
	 * NONE to clear it so the wording names the actor. Null leaves the role as it is.
	 */
	public void setActorRoleName(String actorRoleName);

	/**
	 * #468: the project actor a NAMED step names. On its own it implies NAMED. Null leaves it as
	 * it is.
	 */
	public void setActorName(String actorName);
}
