/*
 * $Id: DeleteActorCommand.java,v 1.1 2008/11/20 09:55:14 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.Actor;

/**
 * @author ron
 */
public interface DeleteActorCommand extends EditCommand {

	/**
	 * Set the actor to delete.
	 * 
	 * @param actor
	 */
	public void setActor(Actor actor);
}
