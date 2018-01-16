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

import java.io.InputStream;

import com.rreganjr.command.Command;
import com.rreganjr.nlp.dictionary.Synset;
import com.rreganjr.nlp.dictionary.SynsetDefinitionWord;

/**
 * An XML processor for the WordNet synset merged gloss parse and word sense
 * files.<br>
 * The processor parses the xml and adds {@link SynsetDefinitionWord}s to the
 * {@link Synset}s<br>
 * 
 * @see {http://wordnet.princeton.edu/glosstag-files/}
 * @author ron
 */
public interface LoadWordNetTaggedGlossesCommand extends Command {

	/**
	 * @param inputStream -
	 *            a stream to a WordNet merged tagged gloss file.
	 * @see {http://wordnet.princeton.edu/glosstag-files/}
	 */
	public void setInputStream(InputStream inputStream);
}
