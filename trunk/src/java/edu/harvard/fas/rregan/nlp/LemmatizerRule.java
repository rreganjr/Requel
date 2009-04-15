/*
 * $Id: LemmatizerRule.java,v 1.1 2008/10/02 09:38:37 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp;

/**
 * interface for specifying rules for lemmatizing a word.
 * 
 * @author ron
 */
public interface LemmatizerRule {

	/**
	 * Given a word and part of speech, apply the rule returning the lemma if
	 * this rule is applicable and a valid lemma is found, or returning null.
	 * 
	 * @param word -
	 *            the word to lemmatize
	 * @param partOfSpeech -
	 *            the part of speech such as NOUN, VERB, etc.
	 * @return the lemma if the rule is appropriate and a valid lemma is
	 *         generated (found in the dictionary), null otherwise.
	 */
	public String lemmatize(String word, PartOfSpeech partOfSpeech);
}