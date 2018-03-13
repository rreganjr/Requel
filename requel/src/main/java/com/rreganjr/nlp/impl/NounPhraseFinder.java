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
package com.rreganjr.nlp.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.rreganjr.nlp.GrammaticalStructureLevel;
import com.rreganjr.nlp.NLPProcessor;
import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.ParseTag;

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
