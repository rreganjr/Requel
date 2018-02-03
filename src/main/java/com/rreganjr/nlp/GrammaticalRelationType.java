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
package com.rreganjr.nlp;

import java.util.HashMap;
import java.util.Map;

/**
 * Grammatical relationships between words in a sentence. Based on the
 * dependency parse from the Stanford Parser.
 * 
 * @see {@link "http://nlp.stanford.edu/software/dependencies_manual.pdf"}
 * @author ron
 */
public enum GrammaticalRelationType {

	GOVERNOR(null, "gov", "governor", ""),
	DEPENDENT(null, "dep", "dependent", ""),
	ROOT(null, "root", "root", "The \"root\" grammatical relation between a faked \"ROOT\" node, and the root of the sentence."),
	PREDICATE(DEPENDENT, "pred", "predicate", ""),
	AUXILIARY(DEPENDENT, "aux", "auxiliary", ""),
	PASSIVE_AUXILIARY(AUXILIARY, "auxpass", "passive auxiliary", ""),
	COPULA(AUXILIARY, "cop", "copula", "a copula is a word used to link the subject of a sentence with a predicate. It is sometimes refered to as a linking verb."),
	CONJUNCT(DEPENDENT, "conj", "conjunct", ""),
	COORDINATION(DEPENDENT, "cc", "coordination", ""),
	PUNCTUATION(DEPENDENT, "punct", "punctuation", ""),
	ARGUMENT(DEPENDENT, "arg", "argument", ""),
	SUBJECT(ARGUMENT, "subj", "subject", ""),
	NOMINAL_SUBJECT(SUBJECT, "nsubj", "nominal subject", ""),
	NOMINAL_PASSIVE_SUBJECT(NOMINAL_SUBJECT, "nsubjpass", "nominal passive subject", ""),
	CLAUSAL_SUBJECT(SUBJECT, "csubj", "clausal subject", ""),
	CLAUSAL_PASSIVE_SUBJECT(CLAUSAL_SUBJECT, "csubjpass", "clausal passive subject", ""),
	COMPLEMENT(ARGUMENT, "comp", "complement", ""),
	OBJECT(COMPLEMENT, "obj", "object", ""),
	DIRECT_OBJECT(OBJECT, "dobj", "direct object", ""),
	INDIRECT_OBJECT(OBJECT, "iobj", "indirect object", ""),
	MODIFIER(DEPENDENT, "mod", "modifier", ""),
	NOMINAL_MODIFIER(MODIFIER, "nmod", "nominal modifier", ""),
	CLAUSAL_COMPLEMENT(COMPLEMENT, "ccomp", "clausal complement", ""),
	XCLAUSAL_COMPLEMENT(COMPLEMENT, "xcomp", "xclausal complement", ""),
	MARKER(MODIFIER, "mark", "marker", ""),
	RELATIVE(COMPLEMENT, "rel", "relative", ""),
	REFERENT(DEPENDENT, "ref", "referent", ""),
	EXPLETIVE(DEPENDENT, "expl", "expletive", ""),
	ADVERBIAL_CLAUSE_MODIFIER(MODIFIER, "advcl", "adverbial clause modifier", ""),
	TEMPORAL_MODIFIER(NOMINAL_MODIFIER, "nmod:tmod", "temporal modifier", ""),
	CLAUSAL_MODIFIER_OF_NOUN(MODIFIER, "acl", "clausal modifier of noun", ""),
	RELATIVE_CLAUSE_MODIFIER(CLAUSAL_MODIFIER_OF_NOUN, "acl:relcl", "relative clause modifier", ""),
	NUMERIC_MODIFIER(MODIFIER, "nummod", "numeric modifier", ""),
	ADJECTIVAL_MODIFIER(MODIFIER, "amod", "adjectival modifier", ""),
	COMPOUND_MODIFIER(MODIFIER, "compound", "compound modifier", ""),
	NAME(MODIFIER, "name", "name", ""),
	APPOSITIONAL_MODIFIER(MODIFIER, "appos", "appositional modifier", ""),
	ADVERBIAL_MODIFIER(MODIFIER, "advmod", "adverbial modifier", ""),
	NEGATION_MODIFIER(ADVERBIAL_MODIFIER, "neg", "negation modifier", ""),
	MULTI_WORD_EXPRESSION(MODIFIER, "mwe", "multi-word expression", ""),
	DETERMINER(MODIFIER, "det", "determiner", ""),
	PREDETERMINER(MODIFIER, "det:predet", "predeterminer", ""),
	PRECONJUNCT(MODIFIER, "cc:preconj", "preconjunct", ""),
	POSSESSION_MODIFIER(MODIFIER, "nmod:poss", "possession modifier", ""),
	CASE_MARKER(MODIFIER, "case", "case marker", ""),
	PHRASAL_VERB_PARTICLE(MODIFIER, "compound:prt", "phrasal verb particle", ""),
	SEMANTIC_DEPENDENT(DEPENDENT, "sdep", "semantic dependent", ""),
	/**
	 * agent: agent<br>
	 * An agent is the complement of a passive verb which is introduced by the
	 * preposition "by" and does the action.<br>
	 * The man has been killed by the police. - agent(killed, police)<br>
	 * Effects caused by the protein are important. - agent(caused, protein)<br>
	 * NOTE: may be returned as the prepositional object:<br>
	 * pobj(by-5, police-7)<br>
	 * det(man-1, The-0)<br>
	 * det(police-7, the-6)<br>
	 * aux(killed-4, has-2)<br>
	 * nsubjpass(killed-4, man-1)<br>
	 * auxpass(killed-4, been-3)<br>
	 * prep(killed-4, by-5)<br>
	 */
	AGENT(DEPENDENT, "agent", "agent", ""),
	NOUN_PHRASE_ADVERBIAL_MODIFIER(MODIFIER, "nmod:npmod", "noun phrase adverbial modifier", ""),
	/**
	 * a relation between the main verb of a clause and other sentential
	 * elements, such as a sentential parenthetical, a clause after a ":" or a
	 * ";".<br>
	 * For example:<br>
	 * "The guy, John said, left early in the morning."<br>
	 * parataxis(left, said)
	 */
	PARATAXIS(DEPENDENT, "parataxis", "parataxis", ""),
	DISCOURSE_ELEMENT(MODIFIER, "discourse", "discourse element", ""),
	GOES_WITH(MODIFIER, "goeswith", "goes with", ""),
	LIST(DEPENDENT, "list", "list", ""),
	PREPOSITION(COMPLEMENT, "prep", "preposition", ""),
	QUANTIFICATIONAL_MODIFIER(DETERMINER, "det:qmod", "quantificational modifier", ""),
	/**
	 * xsubj : controlling subject<br>
	 * A controlling subject is the relation between the head of a open clausal
	 * complement (xcomp) and the external subject of that clause.<br>
	 * Tom likes to eat fish. - xsubj(eat, Tom)
	 */
	CONTROLLING_NOMINAL_SUBJECT(NOMINAL_SUBJECT, "nsubj:xsubj", "controlling nominal subject", ""),
	CONTROLLING_NOMINAL_PASSIVE_SUBJECT(NOMINAL_PASSIVE_SUBJECT, "nsubjpass:xsubj", "controlling nominal passive subject", ""),
	CONTROLLING_CLAUSAL_SUBJECT(NOMINAL_PASSIVE_SUBJECT, "csubj:xsubj", "controlling clausal subject", ""),
	CONTROLLING_CLAUSAL_PASSIVE_SUBJECT(NOMINAL_PASSIVE_SUBJECT, "csubjpass:xsubj", "controlling clausal passive subject", "");

	private static final Map<String, GrammaticalRelationType> grammaticalRelationsByShortName = new HashMap<String, GrammaticalRelationType>();
	static {
		for (GrammaticalRelationType grt : values()) {
			grammaticalRelationsByShortName.put(grt.getShortName(), grt);
		}
	}

	public static GrammaticalRelationType getGrammaticalRelationByShortName(String shortName) {
		return grammaticalRelationsByShortName.get(shortName);
	}

	private final GrammaticalRelationType parent;
	private final String shortName;
	private final String longName;
	private final String description;

	private GrammaticalRelationType(GrammaticalRelationType parent, String shortName,
			String longName, String description) {
		this.parent = parent;
		this.shortName = shortName;
		this.longName = longName;
		this.description = description;
	}

	/**
	 * @return A more general relation that this relation is a subtype of.
	 */
	public GrammaticalRelationType getParent() {
		return parent;
	}

	/**
	 * @return the short name of the relation as returned by the Stanford
	 *         Parser.
	 */
	public String getShortName() {
		return shortName;
	}

	/**
	 * @return A human understandable name.
	 */
	public String getLongName() {
		return longName;
	}

	/**
	 * @return A description of what the relationship means
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param relation -
	 *            the relation to compare with this one to see if this one is
	 *            equal to or a sub type of the supplied relation.
	 * @return true if this relation is equal to or a sub type of the supplied
	 *         relation.
	 */
	public boolean isA(GrammaticalRelationType relation) {
		GrammaticalRelationType thisOrAncestor = this;
		do {
			if (thisOrAncestor.equals(relation)) {
				return true;
			}
			thisOrAncestor = thisOrAncestor.getParent();
		} while (thisOrAncestor != null);
		return false;
	}

	@Override
	public String toString() {
		return getShortName();
	}

}
