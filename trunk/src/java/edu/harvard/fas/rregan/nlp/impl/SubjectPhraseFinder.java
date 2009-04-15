/*
 * $Id: SubjectPhraseFinder.java,v 1.2 2008/07/25 01:17:07 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
