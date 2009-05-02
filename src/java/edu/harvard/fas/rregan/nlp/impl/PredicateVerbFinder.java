/*
 * $Id: PredicateVerbFinder.java,v 1.3 2009/02/06 11:49:15 rregan Exp $
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

package edu.harvard.fas.rregan.nlp.impl;

import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.ParseTag;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;

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
				} else if (child.is(GrammaticalStructureLevel.PHRASE) && child.is(ParseTag.VP)) {
					return process(child);
				}
			}
		}
		return null;
	}
}
