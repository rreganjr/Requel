/*
 * $Id: DependencyPrimaryVerbFinder.java,v 1.3 2009/03/05 08:50:47 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.nlp.GrammaticalRelation;
import edu.harvard.fas.rregan.nlp.GrammaticalRelationType;
import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.ParseTag;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;

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
