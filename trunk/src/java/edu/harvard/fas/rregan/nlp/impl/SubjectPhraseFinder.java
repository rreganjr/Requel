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
package edu.harvard.fas.rregan.nlp.impl;

import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.ParseTag;

/**
 * @author ron
 */
public class SubjectPhraseFinder implements NLPProcessor<NLPText> {

	/**
	 * if the text is a SENTENCE or CLAUSE return the subject noun phrase.
	 */
	@Override
	public NLPText process(NLPText text) {
		NLPText clause = null;
		if (text.is(GrammaticalStructureLevel.SENTENCE)) {
			// first clause holds the subject?
			for (NLPText child : text.getChildren()) {
				if (child.is(GrammaticalStructureLevel.CLAUSE)) {
					clause = child;
					break;
				}
			}
		} else if (text.is(GrammaticalStructureLevel.CLAUSE)) {
			clause = text;
		}
		if (clause != null) {
			for (NLPText child : clause.getChildren()) {
				if (child.is(GrammaticalStructureLevel.PHRASE) && child.is(ParseTag.NP)) {
					return child;
				}
			}
		}
		return null;
	}
}
