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
