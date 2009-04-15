/*
 * $Id: GrammaticalRelationType.java,v 1.10 2009/02/06 11:49:18 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp;

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

	/**
	 * The root relationship indicating that the governor "governs" the
	 * dependent.
	 */
	GOVERNOR(null, "gov", "governor", ""),

	/**
	 * 
	 */
	DEPENDENT(null, "dep", "dependent", ""),

	/**
	 * 
	 */
	PREDICATE(DEPENDENT, "pred", "predicate", ""),

	/**
	 * 
	 */
	AUXILIARY(DEPENDENT, "aux", "auxiliary", ""),

	/**
	 * 
	 */
	PASSIVE_AUXILIARY(AUXILIARY, "auxpass", "passive auxiliary", ""),

	/**
	 * 
	 */
	COPULA(
			AUXILIARY,
			"cop",
			"copula",
			"a copula is a word used to link the subject of a sentence with a predicate. It is sometimes refered to as a linking verb."),

	/**
	 * 
	 */
	CONJUNCT(DEPENDENT, "conj", "conjunct", ""),

	/**
	 * 
	 */
	COORDINATION(DEPENDENT, "cc", "coordination", ""),

	/**
	 * a relation between the main verb of a clause and other sentential
	 * elements, such as a sentential parenthetical, a clause after a ":" or a
	 * ";".<br>
	 * For example:<br>
	 * "The guy, John said, left early in the morning."<br>
	 * parataxis(left, said)
	 */
	PARATAXIS(DEPENDENT, "parataxis", "parataxis", ""),

	/**
	 * 
	 */
	PUNCTUATION(DEPENDENT, "punct", "punctuation", ""),

	/**
	 * 
	 */
	ARGUMENT(DEPENDENT, "arg", "argument", ""),

	/**
	 * 
	 */
	SUBJECT(ARGUMENT, "subj", "subject", ""),

	/**
	 * 
	 */
	NOMINAL_SUBJECT(SUBJECT, "nsubj", "nominal subject",
			"a subject consisting of a noun, and possibly modifiers."),

	/**
	 * 
	 */
	NOMINAL_PASSIVE_SUBJECT(SUBJECT, "nsubjpass", "nominal passive subject", ""),

	/**
	 * csubj : clausal subject<br>
	 * A clausal subject is a clausal syntactic subject of a clause, i.e. the
	 * subject is itself a clause. The governor of this relation might not
	 * always be a verb: when the verb is a copular verb, the root of the clause
	 * is the complement of the copular verb. In the two following examples,
	 * what she said is the subject.<br>
	 * What she said makes sense. - csubj (makes, said)<br>
	 * What she said is not true. - csubj (true, said)<br>
	 */
	CLAUSAL_SUBJECT(
			SUBJECT,
			"csubj",
			"clausal subject",
			"A clausal subject is a clausal syntactic subject of a clause, i.e. the subject is itself a clause."),

	/**
	 * csubjpass: clausal passive subject<br>
	 * A clausal passive subject is a clausal syntactic subject of a passive
	 * clause. In the example below, that she lied is the subject.<br>
	 * That she lied was suspected by everyone. - csubjpass(suspected, lied)<br>
	 */
	CLAUSAL_PASSIVE_SUBJECT(CLAUSAL_SUBJECT, "csubjpass", "clausal passive subject",
			"A clausal passive subject is a clausal syntactic subject of a passive clause."),

	/**
	 * A complement ties an object to a verb in a different phrase or clause
	 */
	COMPLEMENT(
			ARGUMENT,
			"comp",
			"complement",
			"A complement of a VP is any object (direct or indirect) of that VP, or a "
					+ "clause or adjectival phrase which functions like an object; a complement of "
					+ "a clause is an complement of the VP which is the predicate of that clause."),

	/**
	 * 
	 */
	ATTRIBUTIVE(COMPLEMENT, "attr", "attributive", ""),

	/**
	 * 
	 */
	OBJECT(COMPLEMENT, "obj", "object", ""),

	/**
	 * 
	 */
	DIRECT_OBJECT(OBJECT, "dobj", "direct object", ""),

	/**
	 * 
	 */
	INDIRECT_OBJECT(OBJECT, "iobj", "indirect object", ""),

	/**
	 * 
	 */
	PREPOSITIONAL_OBJECT(OBJECT, "pobj", "prepositional object", ""),

	/**
	 * 
	 */
	PREPOSITIONAL_COMPLEMENT(OBJECT, "pcomp", "prepositional complement", ""),

	/**
	 * 
	 */
	CLAUSAL_COMPLEMENT(COMPLEMENT, "ccomp", "clausal complement", ""),

	/**
	 * xcomp: open clausal complement<br>
	 * An open clausal complement (xcomp) of a VP or an ADJP is a clausal
	 * complement without its own subject, whose reference is determined by an
	 * external subject. These complements are always non-finite. The name xcomp
	 * is borrowed from Lexical-Functional Grammar.<br>
	 * He says that you like to swim. - xcomp(like, swim)<br>
	 * I am ready to leave. - xcomp(ready, leave)<br>
	 * John hates eating fish. - xcomp(hates, eating)<br>
	 */
	XCLAUSAL_COMPLEMENT(COMPLEMENT, "xcomp", "xclausal complement",
			"An open clausal complement without its own subject."),

	/**
	 * example: I bet Bob a dollar [that] Pi is a number.
	 */
	COMPLEMENTIZER(COMPLEMENT, "complm", "complementizer",
			"Like a subordinating conjunction used to show the relationship between the two clauses"),

	/**
	 * 
	 */
	MARKER(COMPLEMENT, "mark", "marker", ""),

	/**
	 * 
	 */
	RELATIVE(COMPLEMENT, "rel", "relative", ""),

	/**
	 * 
	 */
	REFERENT(DEPENDENT, "ref", "referent", ""),

	/**
	 * 
	 */
	EXPLETIVE(DEPENDENT, "expl", "expletive", ""),

	/**
	 * acomp: adjectival complement<br>
	 * An adjectival complement of a VP is an adjectival phrase which functions
	 * as the complement (like an object of the verb); an adjectival complement
	 * of a clause is the adjectival complement of the VP which is the predicate
	 * of that clause. <br>
	 * She looks very beautiful. - acomp(looks, beautiful)
	 */
	ADJECTIVAL_COMPLEMENT(COMPLEMENT, "acomp", "adjectival complement",
			"An adjectival complement of a VP is an adjectival phrase which functions as the complement"),

	/**
	 * 
	 */
	MODIFIER(DEPENDENT, "mod", "modifier", ""),

	/**
	 * 
	 */
	ADVERBIAL_CLAUSE_MODIFIER(MODIFIER, "advcl", "adverbial clause modifier", ""),

	/**
	 * 
	 */
	TEMPORAL_MODIFIER(MODIFIER, "tmod", "temporal modifier", ""),

	/**
	 * 
	 */
	RELATIVE_CLAUSE_MODIFIER(MODIFIER, "rcmod", "relative clause modifier", ""),

	/**
	 * 
	 */
	NUMERIC_MODIFIER(MODIFIER, "num", "numeric modifier", ""),

	/**
	 * 
	 */
	ADJECTIVAL_MODIFIER(MODIFIER, "amod", "adjectival modifier", ""),

	/**
	 * 
	 */
	NN_MODIFIER(MODIFIER, "nn", "nn modifier", ""),

	/**
	 * 
	 */
	APPOSITIONAL_MODIFIER(MODIFIER, "appos", "appositional modifier", ""),

	/**
	 * abbrev: abbreviation modifer<br>
	 * An abbreviation modifer of an NP is a parenthesized NP that serves to
	 * abbreviate the NP (or to define an abbreviation).<br>
	 * The Australian Broadcasting Corporation (ABC). - abbrev(Corporation, ABC)
	 */
	ABBREVIATION_MODIFIER(
			APPOSITIONAL_MODIFIER,
			"abbrev",
			"abbreviation modifier",
			"An abbreviation modifer of an NP is a parenthesized NP that serves to abbreviate the NP (or to define an abbreviation)."),

	/**
	 * 
	 */
	PARTICIPIAL_MODIFIER(MODIFIER, "partmod", "participial modifier", ""),

	/**
	 * 
	 */
	INFINITIVAL_MODIFIER(MODIFIER, "infmod", "infinitival modifier", ""),

	/**
	 * 
	 */
	ADVERBIAL_MODIFIER(MODIFIER, "advmod", "adverbial modifier", ""),

	/**
	 * 
	 */
	NEGATION_MODIFIER(ADVERBIAL_MODIFIER, "neg", "negation modifier", ""),

	/**
	 * 
	 */
	DETERMINER(MODIFIER, "det", "determiner", ""),

	/**
	 * 
	 */
	PREDETERMINER(MODIFIER, "predet", "predeterminer", ""),

	/**
	 * 
	 */
	PRECONJUNCT(MODIFIER, "preconj", "preconjunct", ""),

	/**
	 * 
	 */
	POSSESSION_MODIFIER(MODIFIER, "poss", "possession modifier", ""),

	/**
	 * 
	 */
	POSSESSIVE_MODIFIER(MODIFIER, "possessive", "possessive modifier", ""),

	/**
	 * 
	 */
	PREPOSITIONAL_MODIFIER(MODIFIER, "prep", "prepositional modifier", ""),

	/**
	 * 
	 */
	PHRASAL_VERB_PARTICLE(MODIFIER, "prt", "phrasal verb particle", ""),

	/**
	 * 
	 */
	SEMANTIC_DEPENDENT(DEPENDENT, "sdep", "semantic dependent", ""),

	/**
	 * xsubj : controlling subject<br>
	 * A controlling subject is the relation between the head of a open clausal
	 * complement (xcomp) and the external subject of that clause.<br>
	 * Tom likes to eat fish. - xsubj(eat, Tom)
	 */
	CONTROLLING_SUBJECT(
			SEMANTIC_DEPENDENT,
			"xsubj",
			"controlling subject",
			"A controlling subject is the relation between the head of a open clausal complement (xcomp) and the external subject of that clause."),

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

	/**
	 * 
	 */
	COMPOUND_NUMBER_MODIFIER(MODIFIER, "number", "compound number modifier", ""),

	/**
	 * 
	 */
	PURPOSE_CLAUSE_MODIFIER(MODIFIER, "purpcl", "purpose clause modifier", ""),

	/**
	 * 
	 */
	QUANTIFIER_MODIFIER(MODIFIER, "quantmod", "quantifier modifier", ""),

	/**
	 * 
	 */
	MEASURE_PHRASE(MODIFIER, "measure", "measure phrase", "");

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
