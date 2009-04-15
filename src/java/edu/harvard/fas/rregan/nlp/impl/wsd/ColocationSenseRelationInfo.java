/*
 * $Id: ColocationSenseRelationInfo.java,v 1.1 2008/12/14 11:36:12 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.wsd;

import edu.harvard.fas.rregan.nlp.dictionary.Sense;

/**
 * @author ron
 */
public class ColocationSenseRelationInfo extends AbstractSenseRelationInfo {

	/**
	 * Enum defining the source of the colocation.
	 * 
	 * @author ron
	 */
	public static enum ColocationSource {
		/**
		 * The colocation occurs in a disambiguated wordnet synset definition.
		 */
		WordNetDefinition(),

		/**
		 * The colocation occurs in a Semcor Corpus sentence.
		 */
		SemcorCorpus();

	}

	private final ColocationSource colocationSource;

	protected ColocationSenseRelationInfo(ColocationSource colocationSource, Sense sense1,
			Sense sense2, String reason) {
		super(sense1, sense2, 0.25, reason);
		this.colocationSource = colocationSource;
	}

	@Override
	public String toString() {
		return "colocation(" + colocationSource + ", " + getSense1() + ", " + getSense2() + ") -> "
				+ getRank() + " {" + getReason() + "}";
	}
}