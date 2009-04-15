/*
 * $Id: NLPProcessorFactory.java,v 1.17 2009/03/27 07:16:09 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp;

import java.util.Collection;
import java.util.List;

/**
 * @author ron
 */
public interface NLPProcessorFactory {

	/**
	 * @param text -
	 *            The text that will be processed
	 * @return an NLPText used for processing and analysis.
	 */
	public NLPText createNLPText(String text);

	/**
	 * Create a new NLPText for the given text and apply some processors.
	 * 
	 * @param text -
	 *            The text that will be processed
	 * @return an NLPText used for processing and analysis.
	 */
	public NLPText processText(String text);

	/**
	 * @param texts -
	 *            two or more NLPText objects to append together.
	 * @return a new NLPText consisting of the supplied texts
	 */
	public NLPText appendText(NLPText... texts);

	/**
	 * @param texts -
	 *            two or more NLPText objects to append together.
	 * @return a new NLPText consisting of the supplied texts
	 */
	public NLPText appendText(List<NLPText> texts);

	/**
	 * @return an NLPProcessor that takes an NLPText object and generates a
	 *         constituent parse tree returned in a String.
	 */
	public NLPProcessor<String> getConstituentTreePrinter();

	/**
	 * @return an NLPProcessor that takes an NLPText object and generates a
	 *         description of the word dependencies of the text returned in a
	 *         String.
	 */
	public NLPProcessor<String> getDependencyPrinter();

	/**
	 * @return an NLPProcessor that takes an NLPText object and generates a
	 *         description of the semantic roles each node plays for the verbs
	 *         in the text.
	 */
	public NLPProcessor<String> getSemanticRolePrinter();

	/**
	 * @return an NLPProcessor that takes an NLPText object and returns the
	 *         maximum depth of the syntax structure.
	 */
	public NLPProcessor<Integer> getConstituentTreeDepthFinder();

	/**
	 * @return an NLPProcessor that takes an NLPText object and updates it with
	 *         the lemma (base form) of the text word.
	 */
	public NLPProcessor<NLPText> getLemmatizer();

	/**
	 * @return an NLPProcessor that takes an NLPText SENTENCE object, identifies
	 *         tokens if needed and creates the constituent clauses, phrases and
	 *         words that make up the text as well as dependencies between the
	 *         words.
	 */
	public NLPProcessor<NLPText> getParser();

	/**
	 * @return an NLPProcessor that takes an NLPText object and identifies the
	 *         sentences in it if appropriate.
	 */
	public NLPProcessor<NLPText> getSentencizer();

	/**
	 * @return an NLPProcessor that takes an NLPText object and identifies the
	 *         words or tokens that make it up.
	 */
	public NLPProcessor<NLPText> getTokenizer();

	/**
	 * @return an NLPProcessor that takes a NLPText object and indicates the
	 *         semantic roles: including primary verb, agent, patient and theme
	 *         of the constituents.
	 */
	public NLPProcessor<NLPText> getSemanticRoleLabeler();

	/**
	 * @return an NLPProcessor that takes an NLPText object and identifies all
	 *         the noun phrases in it.
	 */
	public NLPProcessor<Collection<NLPText>> getNounPhraseFinder();

	/**
	 * @return an NLPProcessor that takes an NLPWord that maybe misspelled and
	 *         returns a collection of words that may be the correct spelling.
	 */
	public NLPProcessor<Collection<NLPText>> getSimilarWordFinder();

	/**
	 * @return an NLPProcessor that takes an NLPWord with sense information and
	 *         suggests more specific senses (hyponyms.)
	 */
	public NLPProcessor<Collection<NLPText>> getMoreSpecificWordSuggester();

	/**
	 * @return an NLPProcessor that takes an NLPWord and returns true if the
	 *         word is found in the dictionary, or false if not.
	 */
	public NLPProcessor<Boolean> getSpellingChecker();

	/**
	 * @see {@link NLPText#getPrimaryVerb()}
	 * @return an NLPProcessor that takes an NLPText object and identifies the
	 *         primary verb.
	 */
	public NLPProcessor<NLPText> getPrimaryVerbFinder();

	/**
	 * @return an NLPProcessor that takes an NLPText object and disambiguates
	 *         all the words.
	 */
	public NLPProcessor<NLPText> getWordSenseDisambiguator();

	/**
	 * @return an NLPProcessor that takes an NLPText object and links the
	 *         WordNet word reference to each available word in the text.
	 */
	public NLPProcessor<NLPText> getDictionizer();

	/**
	 * @return an NLPProcessor that takes an NLPText object and identifies words
	 *         that represent named entities like people, organizations, and
	 *         locations.
	 */
	public NLPProcessor<NLPText> getNamedEntityResolver();
}
