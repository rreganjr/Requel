/*
 * $Id: DependencySubjectFinder.java,v 1.8 2009/02/08 13:25:15 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl;

import java.util.Set;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.nlp.GrammaticalRelation;
import edu.harvard.fas.rregan.nlp.GrammaticalRelationType;
import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.ParseTag;
import edu.harvard.fas.rregan.nlp.SemanticRole;

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
