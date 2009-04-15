/*
 * $Id: BuildWordNetDefinitionWordsCommand.java,v 1.1 2008/12/13 00:40:11 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary.command;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;

/**
 * Take a Synset and create SynsetDefinitionWords for
 * the word forms defined in the synset wsd field.
 * 
 * @author ron
 */
public interface BuildWordNetDefinitionWordsCommand extends Command {

	/**
	 * 
	 * @param synset - the synset to build the words for
	 */
	public void setSynset(Synset synset);
}
