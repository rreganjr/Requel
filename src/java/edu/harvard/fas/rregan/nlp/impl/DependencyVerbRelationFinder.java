/*
 * $Id: DependencyVerbRelationFinder.java,v 1.2 2009/02/06 11:49:16 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl;

import edu.harvard.fas.rregan.nlp.GrammaticalRelation;
import edu.harvard.fas.rregan.nlp.GrammaticalRelationType;
import edu.harvard.fas.rregan.nlp.NLPText;

/**
 * This uses the dependency information attached to the NLPText to get the
 * phrase containing the word of the supplied relation type of the primary verb
 * of a SENTENCE, CLAUSE, or PHRASE.
 * 
 * @author ron
 */
public class DependencyVerbRelationFinder extends DependencyPrimaryVerbFinder {

	private final GrammaticalRelationType relationType;

	/**
	 * @param relationType
	 */
	public DependencyVerbRelationFinder(GrammaticalRelationType relationType) {
		this.relationType = relationType;
	}

	/**
	 * If the text is a SENTENCE, CLAUSE or PHRASE, return the direct object.
	 * 
	 * @param text -
	 *            a SENTENCE or CLAUSE to find the relation phrase in
	 * @return An NLPText
	 */
	@Override
	public NLPText process(NLPText text) {
		NLPText verb = super.process(text);
		if (verb != null) {
			for (GrammaticalRelation relation : verb.getGovernorOfType(relationType)) {
				return relation.getDependent();
			}
		}
		return null;
	}
}
