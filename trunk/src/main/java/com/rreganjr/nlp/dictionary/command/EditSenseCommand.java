/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.nlp.dictionary.command;

import com.rreganjr.command.Command;
import com.rreganjr.nlp.dictionary.Sense;
import com.rreganjr.nlp.dictionary.Synset;
import com.rreganjr.nlp.dictionary.Word;

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
