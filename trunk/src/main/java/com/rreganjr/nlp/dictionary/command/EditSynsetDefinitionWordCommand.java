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
import com.rreganjr.nlp.ParseTag;
import com.rreganjr.nlp.dictionary.Sense;
import com.rreganjr.nlp.dictionary.Synset;
import com.rreganjr.nlp.dictionary.SynsetDefinitionWord;

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
