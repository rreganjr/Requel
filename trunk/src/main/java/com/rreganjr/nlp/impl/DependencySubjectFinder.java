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

import java.util.Set;

import org.apache.log4j.Logger;

import com.rreganjr.nlp.GrammaticalRelation;
import com.rreganjr.nlp.GrammaticalRelationType;
import com.rreganjr.nlp.GrammaticalStructureLevel;
import com.rreganjr.nlp.NLPProcessor;
import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.ParseTag;
import com.rreganjr.nlp.SemanticRole;

/**
 * This uses the dependency information attached to the NLPText to get the
 * subject of a SENTENCE, CLAUSE, or PHRASE.
 * 
 * @author ron
 */
public class DependencySubjectFinder implements NLPProcessor<NLPText> {
	private static final Logger log = Logger.getLogger(DependencySubjectFinder.class);

	/**
	 * Set the subject of a phrase, clause or sentence.
	 * 
	 * @param text
	 */
	@Override
	public NLPText process(NLPText text) {
		for (NLPText word : text.getLeaves()) {
			Set<GrammaticalRelation> relations = word
					.getDependentOfType(GrammaticalRelationType.NOMINAL_SUBJECT);
			if (!relations.isEmpty()) {
				for (GrammaticalRelation relation : relations) {
					log.info(relation);
					if ((word.getParent() != null)
							&& word.getParent().is(GrammaticalStructureLevel.PHRASE)
							&& word.getParent().is(ParseTag.NP)) {
						word = word.getParent();
					}
					word.setSemanticRole(relation.getGovernor(), SemanticRole.ACTOR);
					break;
				}
			}
		}
		return text;
	}

	/**
	 * @param text1
	 * @param text2
	 * @return the first ancestor in the syntax structure (phrases, clauses,
	 *         sentences) that is common to both NLPText nodes or null if there
	 *         isn't a common ancestor.
	 */
	private NLPText getMostCommonAncestor(NLPText text1, NLPText text2) {
		if ((text1 == null) || (text2 == null)) {
			return null;
		} else if (text1.equals(text2)) {
			return text1;
		}
		for (NLPText text1Ancestor : text1.getAncestors()) {
			for (NLPText text2Ancestor : text2.getAncestors()) {
				if (text1Ancestor.equals(text2Ancestor)) {
					return text1Ancestor;
				}
			}
		}
		return null;
	}
}
