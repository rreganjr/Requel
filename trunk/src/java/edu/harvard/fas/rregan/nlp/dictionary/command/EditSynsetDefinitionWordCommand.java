/*
 * $Id: EditSynsetDefinitionWordCommand.java,v 1.1 2008/12/13 00:40:07 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary.command;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.nlp.ParseTag;
import edu.harvard.fas.rregan.nlp.dictionary.Sense;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;
import edu.harvard.fas.rregan.nlp.dictionary.SynsetDefinitionWord;

/**
 * create/edit a SynsetDefinitionWord and update the Synset.
 * 
 * @author ron
 */
public interface EditSynsetDefinitionWordCommand extends Command {

	/**
	 * @param word -
	 *            set an existing definition word to edit, or leave null to
	 *            create a new word.
	 */
	public void setSynsetDefinitionWord(SynsetDefinitionWord word);

	/**
	 * @return the new SynsetDefinitionWord
	 */
	public SynsetDefinitionWord getSynsetDefinitionWord();

	/**
	 * @param synset -
	 *            the synset to build the words for
	 */
	public void setSynset(Synset synset);

	/**
	 * @param sense
	 */
	public void setSense(Sense sense);

	/**
	 * @param index
	 */
	public void setIndex(Integer index);

	/**
	 * @param text
	 */
	public void setText(String text);

	/**
	 * @param parseTag
	 */
	public void setParseTag(ParseTag parseTag);
}
