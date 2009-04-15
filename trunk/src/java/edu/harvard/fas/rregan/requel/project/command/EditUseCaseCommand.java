/*
 * $Id: EditUseCaseCommand.java,v 1.5 2009/01/27 09:30:17 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
