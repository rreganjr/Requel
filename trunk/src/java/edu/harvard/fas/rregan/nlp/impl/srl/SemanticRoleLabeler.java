/*
 * $Id: SemanticRoleLabeler.java,v 1.8 2009/02/11 09:02:54 rregan Exp $
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
package edu.harvard.fas.rregan.nlp.impl.srl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.nlp.GrammaticalRelation;
import edu.harvard.fas.rregan.nlp.GrammaticalRelationType;
import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.ParseTag;
import edu.harvard.fas.rregan.nlp.SemanticRole;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetFrame;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetFrameRef;

/**
 * Identify the child elements of a sentence that fill specific
 * thematic/semantic roles.<br>
 * <br>
 * Roles:<br>
 * Verb: The action.<br>
 * Agent: The causer of the action.<br>
 * Patient: The target/recipient of the action.<br>
 * Theme: The thing being acted apon.<br>
 * <br>
 * Example:<br>
 * I gave Bob fifty dollars.<br>
 * Verb: gave<br>
 * Agent: I<br>
 * Patient: Bob<br>
 * Theme: fifty dollars.<br>
 * 
 * @author ron
 */
@Component("semanticRoleLabeler")
public class SemanticRoleLabeler implements NLPProcessor<NLPText> {
	private static final Logger log = Logger.getLogger(SemanticRoleLabeler.class);
	private static final Map<String, SyntaxMatchingContext> compiledContexts = new HashMap<String, SyntaxMatchingContext>();
	private final DictionaryRepository dictionaryRepository;
	private final VerbNetFrameSyntaxParser vnFrameSyntaxParser;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public SemanticRoleLabeler(DictionaryRepository dictionaryRepository,
			VerbNetFrameSyntaxParser vnFrameSyntaxParser) {
		this.dictionaryRepository = dictionaryRepository;
		this.vnFrameSyntaxParser = vnFrameSyntaxParser;
	}

	/**
	 * @param text
	 */
	@Override
	public NLPText process(NLPText text) {
		if (text.in(GrammaticalStructureLevel.PARAGRAPH)) {
			for (NLPText sentence : text.getChildren()) {
				process(sentence);
			}
		} else {
			if (text.getPrimaryVerb() != null) {
				NLPText primaryVerb = text.getPrimaryVerb();
				boolean rolesAssigned = false;
				if ((primaryVerb.getDictionaryWordSense() != null)
						&& (primaryVerb.getDictionaryWordSense().getVerbNetFrames().size() > 0)) {
					rolesAssigned = assignRolesByVerbNetFrames(text, primaryVerb);
				}
				if (!rolesAssigned) {
					assignRolesBySyntaxDependencies(text, primaryVerb);
				}
			}
		}
		return text;
	}

	protected DictionaryRepository getDictionaryRepository() {
		return dictionaryRepository;
	}

	protected GrammaticalRelation findPrimarySubjectRelation(NLPText text) {
		for (GrammaticalRelation relation : text.getGrammaticalRelations()) {
			GrammaticalRelationType relType = relation.getType();
			if (relType.isA(GrammaticalRelationType.NOMINAL_SUBJECT)
					|| relType.isA(GrammaticalRelationType.AGENT)
					|| relType.isA(GrammaticalRelationType.NOMINAL_PASSIVE_SUBJECT)) {
				return relation;
			}
		}
		throw SemanticRoleLabelerException.nominalSubjectNotFound(text);
	}

	protected synchronized SyntaxMatchingContext getSyntaxMatchingContext(VerbNetFrameRef frameRef) {
		VerbNetFrame frame = frameRef.getFrame();
		SyntaxMatchingContext context = compiledContexts.get(frame.getSyntax());
		if (context == null) {
			context = new SyntaxMatchingContext(frameRef, vnFrameSyntaxParser
					.parseVerbNetFrameSyntax(frameRef));
			compiledContexts.put(frame.getSyntax(), context);
		}
		return context;
	}

	protected boolean assignRolesByVerbNetFrames(NLPText sentence, NLPText primaryVerb) {
		for (VerbNetFrameRef frameRef : primaryVerb.getDictionaryWordSense().getVerbNetFrameRefs()) {
			SyntaxMatchingContext context = getSyntaxMatchingContext(frameRef);
			if (context.matches(getDictionaryRepository(), sentence, primaryVerb)) {
				log.debug("matched verbnet frameRef " + frameRef.getId() + " syntax: "
						+ frameRef.getFrame().getSyntax());
				return true;
			}
		}
		return false;
	}

	private void assignRolesBySyntaxDependencies(NLPText sentence, NLPText primaryVerb) {
		boolean passive = false;
		NLPText subject = null;
		NLPText directObject = null;
		NLPText indirectObject = null;
		NLPText prepObject = null;

		for (GrammaticalRelation relation : sentence.getGrammaticalRelations()) {
			log.debug(relation);
			GrammaticalRelationType relType = relation.getType();
			if (relType.isA(GrammaticalRelationType.SUBJECT)) {
				if (relType.isA(GrammaticalRelationType.NOMINAL_PASSIVE_SUBJECT)) {
					passive = true;
				}
				subject = relation.getDependent();
				if ((subject.getParent() != null) && subject.getParent().is(ParseTag.NP)) {
					subject = subject.getParent();
				}
			}
			if (relType.isA(GrammaticalRelationType.DIRECT_OBJECT)) {
				if ((sentence.getPrimaryVerb() == null)
						|| sentence.getPrimaryVerb().equals(relation.getGovernor())) {
					directObject = relation.getDependent();
					if ((directObject.getParent() != null)
							&& directObject.getParent().is(ParseTag.NP)) {
						directObject = directObject.getParent();
					}
				}
			} else if (relType.isA(GrammaticalRelationType.INDIRECT_OBJECT)) {
				if ((sentence.getPrimaryVerb() == null)
						|| sentence.getPrimaryVerb().equals(relation.getGovernor())) {
					indirectObject = relation.getDependent();
					if ((indirectObject.getParent() != null)
							&& indirectObject.getParent().is(ParseTag.NP)) {
						indirectObject = indirectObject.getParent();
					}
				}
			} else if (relType.isA(GrammaticalRelationType.PREPOSITIONAL_OBJECT)) {
				// use the object of a prepositional phrase if it modifies
				// the primary verb
				Set<GrammaticalRelation> prepRels = relation.getGovernor().getDependentOfType(
						GrammaticalRelationType.PREPOSITIONAL_MODIFIER);
				if (prepRels.size() == 1) {
					NLPText verbPred = prepRels.iterator().next().getGovernor();
					if ((sentence.getPrimaryVerb() == null)
							|| sentence.getPrimaryVerb().equals(verbPred)) {
						// TODO: Should the object be the whole preposition?
						prepObject = relation.getDependent();
						if ((prepObject.getParent() != null)
								&& prepObject.getParent().is(ParseTag.NP)) {
							prepObject = prepObject.getParent();
						}
					}
				}
			}
		}
		if (passive) {
			if ((subject != null) && (directObject != null)) {
				// if the verb is preceeded by "was" or is in perfect
				subject.setSemanticRole(primaryVerb, SemanticRole.PATIENT);
				directObject.setSemanticRole(primaryVerb, SemanticRole.AGENT);
			} else {
				if (subject != null) {
					subject.setSemanticRole(primaryVerb, SemanticRole.AGENT);
				}
				if (directObject != null) {
					directObject.setSemanticRole(primaryVerb, SemanticRole.PATIENT);
				}
			}
		} else {
			if (subject != null) {
				subject.setSemanticRole(primaryVerb, SemanticRole.AGENT);
			}
			if (directObject != null) {
				directObject.setSemanticRole(primaryVerb, SemanticRole.PATIENT);
			}
		}
		if (indirectObject != null) {
			indirectObject.setSemanticRole(primaryVerb, SemanticRole.PATIENT);
		}
		if (prepObject != null) {
			prepObject.setSemanticRole(primaryVerb, SemanticRole.PARTICIPANT);
		}
	}
}
