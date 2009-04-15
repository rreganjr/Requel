/*
 * $Id: SemanticSimilaritySenseRelationInfo.java,v 1.1 2008/12/14 11:36:15 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.wsd;

import edu.harvard.fas.rregan.nlp.dictionary.Sense;

/**
 * @author ron
 */
public class SemanticSimilaritySenseRelationInfo extends AbstractSenseRelationInfo {

	protected SemanticSimilaritySenseRelationInfo(Sense sense1, Sense sense2, double similarity,
			String reason) {
		super(sense1, sense2, similarity, reason);
	}

	@Override
	public String toString() {
		return "similarity(" + getSense1() + ", " + getSense2() + ") -> " + getRank() + " {"
				+ getReason() + "}";
	}
}