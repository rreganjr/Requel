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
import com.rreganjr.nlp.dictionary.Synset;

/**
 * Take a Synset and create SynsetDefinitionWords for the word forms defined in
 * the synset wsd field.
 * 
 * @author ron
 */
public interface BuildWordNetDefinitionWordsCommand extends Command {

	/**
	 * @param synset -
	 *            the synset to build the words for
	 */
	public void setSynset(Synset synset);
}