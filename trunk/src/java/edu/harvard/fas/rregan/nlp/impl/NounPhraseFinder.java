/*
 * $Id: NounPhraseFinder.java,v 1.2 2009/02/09 10:12:28 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
