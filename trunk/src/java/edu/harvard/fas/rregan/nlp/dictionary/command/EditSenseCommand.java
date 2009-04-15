/*
 * $Id: EditSenseCommand.java,v 1.1 2008/12/13 00:40:09 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary.command;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.nlp.dictionary.Sense;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;
import edu.harvard.fas.rregan.nlp.dictionary.Word;

/**
 * @author ron
 */
public interface EditSenseCommand extends Command {

	/**
	 * set the sense to edit or null for a new sense.
	 * 
	 * @param sense
	 */
	public void setSense(Sense sense);

	/**
	 * @return the new or updated sense.
	 */
	public Sense getSense();

	/**
	 * set the word for the sense.
	 * 
	 * @param word
	 */
	public void setWord(Word word);

	/**
	 * set the synset (meaning) for the sense.
	 * 
	 * @param synset
	 */
	public void setSynset(Synset synset);

	/**
	 * set the rank (typically in order of usage) of the sense.
	 * 
	 * @param rank
	 */
	public void setRank(Integer rank);

	/**
	 * set the number of times the word was found in the sample sentences.
	 * 
	 * @param sampleFrequency
	 */
	public void setSampleFrequency(Integer sampleFrequency);

	/**
	 * Set the sense key used in WordNet to identify the sense.
	 * 
	 * @param senseKey
	 */
	public void setSenseKey(String senseKey);
}
