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

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.GrammaticalRelation;
import com.rreganjr.nlp.GrammaticalRelationType;
import com.rreganjr.nlp.GrammaticalStructureLevel;
import com.rreganjr.nlp.NLPProcessor;
import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.ParseTag;
import com.rreganjr.nlp.PartOfSpeech;

/**
 * This uses the dependency information attached to the NLPText to get the
 * primary verb of a SENTENCE, CLAUSE, or PHRASE.
 * 
 * @author ron
 */
@Component("dependencyPrimaryVerbFinder")
public class DependencyPrimaryVerbFinder implements NLPProcessor<NLPText> {
	private static final Logger log = Logger.getLogger(DependencyPrimaryVerbFinder.class);

	/**
	 * Create a new instance. There is no configuration.
	 */
	public DependencyPrimaryVerbFinder() {
	}

	/**
	 * Set the primary verb in a phrase, clause or sentence on the supplied
	 * NLPText and return the updated NLPText. The primary verb is set on all
	 * the ancestors common to the primary verb and subject up to the sentence
	 * level. The primary verb is determined from the dependency parse as the
	 * governer of the nominal subject.
	 * 
	 * @param text -
	 *            the NLPText to find the primary verb of.
	 * @see {@link NLPText#getPrimaryVerb()}
	 * @see {@link GrammaticalRelation}
	 * @see {@link DependencyPrinter}
	 */
	@Override
	public NLPText process(NLPText text) {
		if (text.hasText()) {
			if (GrammaticalStructureLevel.PARAGRAPH.equals(text.getGrammaticalStructureLevel())) {
				for (NLPText sentence : text.getChildren()) {
					process(sentence);
				}
			} else {
				try {
					GrammaticalRelation relation = findPrimarySubjectRelation(text);
					NLPText verb = relation.getGovernor();
					text.setPrimaryVerb(verb);
					// TODO: The parser may have mis-marked the element?
					if (!verb.in(ParseTag.VB, ParseTag.VBD, ParseTag.VBG, ParseTag.VBN,
							ParseTag.VBP, ParseTag.VBZ)) {
						log.debug("fixing mistagged text: " + verb + " " + verb.getParseTag() + " "
								+ verb.getPartOfSpeech());
						((NLPTextImpl) verb).setParseTag(ParseTag.VB);
						((NLPTextImpl) verb).setPartOfSpeech(PartOfSpeech.VERB);
						if ((verb.getParent() != null) && !verb.getParent().is(ParseTag.VP)) {
							((NLPTextImpl) verb.getParent()).setParseTag(ParseTag.VP);
						}
					}
				} catch (NLPProcessorException e) {
					log.warn(e.toString());
				}
			}
		}
		return text;
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
		throw NLPProcessorException.noPrimaryVerb(text);
	}
}
