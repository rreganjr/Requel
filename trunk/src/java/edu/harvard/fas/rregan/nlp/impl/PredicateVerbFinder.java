/*
 * $Id: PredicateVerbFinder.java,v 1.3 2009/02/06 11:49:15 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl;

import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.ParseTag;

/**
 * @author ron
 */
public class PredicateVerbFinder extends PrimaryVerbPhraseFinder {

	/**
	 * If the text is a SENTENCE, CLAUSE or PHRASE, return the primary verb.
	 * 
	 * @param text -
	 *            a SENTENCE or CLAUSE to find the predicate verb of
	 */
	@Override
	public NLPText process(NLPText text) {
		NLPText predicatePhrase = null;
		if (text.is(GrammaticalStructureLevel.CLAUSE)
				|| text.is(GrammaticalStructureLevel.SENTENCE)) {
			predicatePhrase = super.process(text);
		} else if (text.is(GrammaticalStructureLevel.PHRASE) && text.is(ParseTag.VP)) {
			predicatePhrase = text;
		}
		if (predicatePhrase != null) {
			for (NLPText child : predicatePhrase.getChildren()) {
				if (child.is(GrammaticalStructureLevel.WORD) && child.is(PartOfSpeech.VERB)) {
					return child;
				} else if (child.is(GrammaticalStructureLevel.PHRASE)
						&& child.is(ParseTag.VP)) {
					return process(child);
				}
			}
		}
		return null;
	}
}
