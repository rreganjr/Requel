/*
 * $Id: EditSynsetCommand.java,v 1.1 2008/12/13 00:40:10 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary.command;


import java.util.Map;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;

/**
 * @author ron
 */
public interface EditSynsetCommand extends Command {
	/**
	 * @param synset -
	 *            the synset to build the words for
	 */
	public void setSynset(Synset synset);

	/**
	 * Set the number of subsumers of this Synset for each relation/link type.
	 * 
	 * @param subsumerCounts
	 */
	public void setSubsumerCounts(Map<Long, Integer> subsumerCounts);
}
