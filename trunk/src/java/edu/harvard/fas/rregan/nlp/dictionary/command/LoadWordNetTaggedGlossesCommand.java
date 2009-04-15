/*
 * $Id: LoadWordNetTaggedGlossesCommand.java,v 1.1 2008/12/13 00:40:08 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary.command;

import java.io.InputStream;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;
import edu.harvard.fas.rregan.nlp.dictionary.SynsetDefinitionWord;

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
