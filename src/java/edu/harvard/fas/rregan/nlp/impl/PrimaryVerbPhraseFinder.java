/*
 * $Id: PrimaryVerbPhraseFinder.java,v 1.1 2009/02/06 11:49:16 rregan Exp $
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
public class PrimaryVerbPhraseFinder implements NLPProcessor<NLPText> {

	/**
	 * if the text is a SENTENCE or CLAUSE return the primary verb phrase.
	 */
	@Override
	public NLPText process(NLPText text) {
		// TODO: use a processor to mark the primary verb based on verbnet
		// syntax structures if available
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
				if (child.is(GrammaticalStructureLevel.PHRASE) && child.is(ParseTag.VP)) {
					return child;
				}
			}
		}
		return null;
	}

}
