/*
 * $Id: CopyActorCommand.java,v 1.1 2008/09/26 01:35:03 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.Actor;

/**
 * Given a actor, create a new actor copying references to actors and goals.
 * If a name isn't supplied a unique name is generated.
 * 
 * @author ron
 */
public interface CopyActorCommand extends EditCommand {

	/**
	 * @param Actor -
	 *            the Actor to copy.
	 */
	public void setOriginalActor(Actor actor);

	/**
	 * @param newName -
	 *            the name for the new actor. if this is not set, or is set to
	 *            a name already in use, a unique name will be generated.
	 */
	public void setNewActorName(String newName);

	/**
	 * @return the new copy of the use case.
	 */
	public Actor getNewActor();
}
