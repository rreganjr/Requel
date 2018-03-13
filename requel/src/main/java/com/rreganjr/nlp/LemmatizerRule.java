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
package com.rreganjr.nlp;

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