/*
 * $Id: DeleteStoryCommand.java,v 1.1 2008/11/20 09:55:14 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.Story;

/**
 * @author ron
 */
public interface DeleteStoryCommand extends EditCommand {

	/**
	 * Set the story to delete.
	 * 
	 * @param story
	 */
	public void setStory(Story story);
}
