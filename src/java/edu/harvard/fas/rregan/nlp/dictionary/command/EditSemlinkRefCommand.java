/*
 * $Id: EditSemlinkRefCommand.java,v 1.1 2008/12/13 00:40:06 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary.command;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.nlp.dictionary.Semlinkref;

/**
 * @author ron
 */
public interface EditSemlinkRefCommand extends Command {

	public void setSemlinkRef(Semlinkref semlinkref);

	public Semlinkref getSemlinkref();

	public void setFromSynset(Long synsetid);

	public void setToSynset(Long synsetid);

	public void setLinkDef(Long linkdefid);

	public void setDistance(Integer distance);
}
