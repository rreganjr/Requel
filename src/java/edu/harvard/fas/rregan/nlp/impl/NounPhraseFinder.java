/*
 * $Id: NounPhraseFinder.java,v 1.2 2009/02/09 10:12:28 rregan Exp $
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

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.ParseTag;

/**
 * Return a collection of NLPText that are all the noun phrases in the given
 * NLPText.
 * 
 * @author ron
 */
@Component("nounPhraseFinder")
public class NounPhraseFinder implements NLPProcessor<Collection<NLPText>> {

	public NounPhraseFinder() {
	}

	/**
	 * if the text is a SENTENCE, CLAUSE or PHRASE, return all the subordinate
	 * noun phrases.
	 */
	@Override
	public Collection<NLPText> process(NLPText text) {
		Collection<NLPText> nounPhrases = new ArrayList<NLPText>();

		if (!text.is(GrammaticalStructureLevel.WORD)) {
			for (NLPText child : text.getChildren()) {
				nounPhrases.addAll(process(child));
			}
		}

		if (text.is(GrammaticalStructureLevel.PHRASE) && ParseTag.NP.equals(text.getParseTag())) {
			nounPhrases.add(text);
		}
		return nounPhrases;
	}
}
