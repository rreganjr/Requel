/*
 * $Id: WordNet.java,v 1.1 2008/12/08 04:38:33 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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

package edu.harvard.fas.rregan.nlp.impl.wordnet;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.dictionary.Dictionary;

import org.apache.log4j.Logger;

import com.nexagis.jawbone.filter.SimilarFilter;

/**
 * @author ron
 */
public class WordNet {
	private static final Logger log = Logger.getLogger(WordNet.class);

	/**
	 * The name of the property in the NLPImpl.properties file that contains the
	 * path to the JWNL configuration/properties file relative to the classpath.
	 * By default the path is "resources/nlp/jwnl/file_properties.xml"
	 */
	public static final String PROP_JWNL_WORDNET_PROP_FILE = "JWNLPropertyFile";

	private Dictionary jwnlDictionary;

	public WordNet() {
		log.debug("initializing WordNet.");
		String propertiesFile = "resources/nlp/jwnl/file_properties.xml";
		InputStream propertiesStream = getClass().getClassLoader().getResourceAsStream(
				propertiesFile);
		try {
			JWNL.initialize(propertiesStream);
			jwnlDictionary = Dictionary.getInstance();
		} catch (Exception e) {
			log.error("Problem initializing WordNet: " + e, e);
		}
	}

	// return the POS for the supplied Penn Treebank tag string or words "Verb"
	// "Noun" etc.
	protected POS getPartOfSpeechFromString(String partOfSpeech) {
		POS pos = POS.getPOSForLabel(partOfSpeech);
		if (pos == null) {
			if (partOfSpeech.startsWith("V")) {
				pos = POS.VERB;
			} else if (partOfSpeech.startsWith("N")) {
				pos = POS.NOUN;
			} else if (partOfSpeech.startsWith("J")) {
				pos = POS.ADJECTIVE;
			} else if (partOfSpeech.startsWith("RB")) {
				pos = POS.ADVERB;
			}
		}
		log.debug(pos == null ? "null" : pos.toString());
		return pos;
	}

	public void bla(String pos, String lemma) {
		try {
			IndexWord iw = jwnlDictionary.lookupIndexWord(getPartOfSpeechFromString(pos), lemma);
		} catch (Exception e) {
			log.error(e, e);
		}
	}

	public boolean isSearchablePartOfSpeech(String partOfSpeech) {
		return getPartOfSpeechFromString(partOfSpeech) != null;
	}

	public boolean knownWord(String partOfSpeech, String word) {
		log.debug("partOfSpeech = " + partOfSpeech + " word = " + word);
		if (jwnlDictionary != null) {
			try {
				POS pos = getPartOfSpeechFromString(partOfSpeech);
				return jwnlDictionary.getMorphologicalProcessor().lookupBaseForm(pos, word) != null;
			} catch (Exception e) {
				log.error("Problem in JWNL lookupBaseForm(): " + e, e);
			}
		}
		return false;
	}

	public Set<String> similarWords(String partOfSpeech, String word) {
		return similarWords(partOfSpeech, word, 2, 20);
	}

	public Set<String> similarWords(String partOfSpeech, String word, int maxDistance,
			int maxResults) {
		log.debug("partOfSpeech = " + partOfSpeech + " word = " + word + " maxDistance = "
				+ maxDistance + " maxResults = " + maxResults);
		Set<String> similarWords = new TreeSet<String>();
		if (jwnlDictionary != null) {
			try {
				POS pos = getPartOfSpeechFromString(partOfSpeech);
				// this is slow, but find the most similar words first
				if (pos != null) {
					for (int distance = 0; distance <= maxDistance; distance++) {
						if ((maxResults != 0) && (similarWords.size() < maxResults)) {
							SimilarFilter filter = new SimilarFilter(word, true, distance);
							Iterator<IndexWord> iter = jwnlDictionary.getIndexWordIterator(pos);
							while (iter.hasNext()) {
								IndexWord nextWord = iter.next();
								if (filter.accept(nextWord.getLemma())) {
									similarWords.add(nextWord.getLemma());
								}
								if ((maxResults > 0) && (similarWords.size() == maxResults)) {
									break;
								}
							}
						}
					}
				}
			} catch (Exception e) {
				log.error("Problem in JWNL lookupBaseForm(): " + e, e);
			}
		}
		return similarWords;
	}

	public String lemmatize(String partOfSpeech, String word) {
		log.debug("partOfSpeech = " + partOfSpeech + " word = " + word);
		if (jwnlDictionary != null) {
			try {
				POS pos = getPartOfSpeechFromString(partOfSpeech);
				if (pos != null) {
					IndexWord indexWord = jwnlDictionary.getMorphologicalProcessor()
							.lookupBaseForm(pos, word);
					if (indexWord != null) {
						String lemma = indexWord.getLemma();
						log.debug("partOfSpeech = " + partOfSpeech + " word = " + word
								+ " lemma = " + lemma);
						return lemma;
					}
				}
			} catch (Exception e) {
				log.error("Problem in JWNL lookupBaseForm(): " + e, e);
			}
		}
		return word;
	}
}
