/*
 * $Id: DeleteUseCaseCommand.java,v 1.1 2008/11/20 09:55:13 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.UseCase;

/**
 * @author ron
 */
public interface DeleteUseCaseCommand extends EditCommand {

	/**
	 * Set the usecase to delete.
	 * 
	 * @param usecase
	 */
	public void setUseCase(UseCase usecase);
}
