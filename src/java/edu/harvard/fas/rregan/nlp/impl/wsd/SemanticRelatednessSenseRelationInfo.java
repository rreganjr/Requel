/*
 * $Id: SemanticRelatednessSenseRelationInfo.java,v 1.1 2008/12/14 11:36:14 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.wsd;

import edu.harvard.fas.rregan.nlp.dictionary.Sense;

/**
 * @author ron
 */
public class SemanticRelatednessSenseRelationInfo extends AbstractSenseRelationInfo {

	protected SemanticRelatednessSenseRelationInfo(Sense sense1, Sense sense2, double relatedness,
			String reason) {
		super(sense1, sense2, relatedness, reason);
	}

	@Override
	public String toString() {
		return "relatedness(" + getSense1() + ", " + getSense2() + ") -> " + getRank() + " {"
				+ getReason() + "}";
	}
}