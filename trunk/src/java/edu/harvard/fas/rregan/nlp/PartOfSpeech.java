/*
 * $Id$
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

package edu.harvard.fas.rregan.nlp;

/**
 * The part of speech indicates how a word or token is used in a sentence.
 * 
 * @author ron
 */
public enum PartOfSpeech {
	/**
	 * Default if the part of speech is unknown because the word is not
	 * recognized or if the word is ambiguous.
	 */
	UNKNOWN(),

	/**
	 * modal modifiers such as should, must
	 */
	MODAL(),

	/**
	 * 
	 */
	VERB(),

	/**
	 * 
	 */
	NOUN(),

	/**
	 * 
	 */
	PRONOUN(),

	/**
	 * A sub class of NOUNs representing a name. Proper nouns are identified to
	 * facilitate spelling and identification of key terms.
	 */
	PROPERNOUN(),

	/**
	 * A sub class of NOUNs for numeric values
	 */
	NUMBER(),

	/**
	 * 
	 */
	ADJECTIVE(),

	/**
	 * 
	 */
	ADVERB(),

	/**
	 * 
	 */
	PREPOSITION(),

	/**
	 * 
	 */
	CONJUNCTION(),

	/**
	 * 
	 */
	INTERJECTION(),

	/**
	 * functional markers that delimit or demarcate parts of a sentance.
	 */
	PUNCTUATION(),

	/**
	 * Non word symbols that aren't punctuation, like math symbols. Symbols may
	 * be marked lexically by the parser, for example in the "Bracketing
	 * Guidelines for Treebank II Style" pg 58. an equals may be labelled as a
	 * VP as shown in the following example:<br>
	 * (S (NP-SBJ x) (VP = (NP 3)))
	 */
	SYMBOL(),

	/**
	 * For example "the", "a", "all", "any", "which"
	 * 
	 * @see http://en.wikipedia.org/wiki/Determiner_%28class%29
	 */
	DETERMINER();

	public boolean in(PartOfSpeech... poss) {
		for (PartOfSpeech pos : poss) {
			if (equals(pos)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Convert the wordnet part of speech field to a PartOfSpeech enum value.
	 * WordNet uses n - noun, v - verb, a - adjective, r - adverb, s - adjective
	 * satellite
	 * 
	 * @param wordNetPOS
	 * @return
	 */
	public static PartOfSpeech fromWordNetPOS(String wordNetPOS) {
		if ("n".equals(wordNetPOS)) {
			return NOUN;
		} else if ("v".equals(wordNetPOS)) {
			return VERB;
		} else if ("r".equals(wordNetPOS)) {
			return ADVERB;
		} else if ("a".equals(wordNetPOS)) {
			return ADJECTIVE;
		} else if ("s".equals(wordNetPOS)) {
			return ADJECTIVE;
		}
		return UNKNOWN;
	}

	/**
	 * @param partOfSpeech
	 * @return The WordNet part of speech string for the supplied part of speech
	 *         object.
	 */
	public static String toWordNetPOS(PartOfSpeech partOfSpeech) {
		if (partOfSpeech.equals(NOUN)) {
			return "n";
		} else if (partOfSpeech.equals(VERB)) {
			return "v";
		} else if (partOfSpeech.equals(ADVERB)) {
			return "a";
		} else if (partOfSpeech.equals(ADJECTIVE)) {
			// TODO: satellite adjectives use "s"
			return "r";
		}
		return null;

	}
}
