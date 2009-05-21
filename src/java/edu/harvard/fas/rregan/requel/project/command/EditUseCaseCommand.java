/*
 * $Id$
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

import java.util.List;

import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.UseCase;

/**
 * @author ron
 */
public interface EditUseCaseCommand extends EditTextEntityCommand {

	/**
	 * Set the project or domain this usecase is a part of.
	 * 
	 * @param projectOrDomain
	 */
	public void setProjectOrDomain(ProjectOrDomain projectOrDomain);

	/**
	 * Set the primary actor of the usecase by name.
	 * 
	 * @param actorName
	 */
	public void setPrimaryActorName(String actorName);

	/**
	 * Used to pass in an existing usecase for editing purposes. If no usecase
	 * is supplied a new one will be created.
	 * 
	 * @param usecase
	 */
	public void setUseCase(UseCase usecase);

	/**
	 * Get the usecase created or edited via the command. If a usecase was
	 * supplied via setUseCase the returned usecase will represent the same
	 * entity although it may not be equal (for example the user or name has
	 * changed.)
	 * 
	 * @return
	 */
	public UseCase getUseCase();

	/**
	 * @param editStepCommands -
	 *            a list of step or scenario edit commands corresponding to the
	 *            steps of the usecase scenario to execute and then add to the
	 *            usecase.
	 */
	public void setStepCommands(List<EditScenarioStepCommand> editStepCommands);

}
