/*
 * $Id: CopyUseCaseCommand.java,v 1.2 2008/09/26 01:35:03 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.UseCase;

/**
 * Given a use case, create a new use case copying references to actors, goals
 * and stories. If a name isn't supplied a unique name is generated.
 * 
 * @author ron
 */
public interface CopyUseCaseCommand extends EditCommand {

	/**
	 * @param usecase -
	 *            the usecase to copy.
	 */
	public void setOriginalUseCase(UseCase usecase);

	/**
	 * @param newName -
	 *            the name for the new usecase. if this is not set, or is set to
	 *            a name already in use, a unique name will be generated.
	 */
	public void setNewUseCaseName(String newName);

	/**
	 * @return the new copy of the use case.
	 */
	public UseCase getNewUseCase();
}
