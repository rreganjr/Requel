/*
 * $Id: ParseTag.java,v 1.5 2009/02/11 09:02:56 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper for Penn Treebank tags. Adapted from text at
 * http://bulba.sdsu.edu/jeanette/thesis/PennTags.html by JEANETTE PETTIBONE
 * pettibon@stanford.edu
 * 
 * @author rreganjr@users.sourceforge.net
 */
public enum ParseTag {

	/**
	 * 
	 */
	ROOT("ROOT", GrammaticalStructureLevel.SENTENCE, "A Stanford parser specific tag"),

	// Clause Level Tags

	/**
	 * A simple declarative clause.
	 */
	S("S", GrammaticalStructureLevel.CLAUSE,
			"simple declarative clause, i.e. one that is not introduced"
					+ " by a (possible empty) subordinating conjunction or a wh-word"
					+ " and that does not exhibit subject-verb inversion."),

	/**
	 * A clause introduced by a subordinating conjunction.
	 */
	SBAR("SBAR", GrammaticalStructureLevel.CLAUSE,
			"Relative and subordinate clauses introduced by a (possibly empty) subordinating conjunction."),

	/**
	 * A direct question introduced by a wh-word or a wh-phrase.
	 */
	SBARQ("SBARQ", GrammaticalStructureLevel.CLAUSE,
			"Direct question introduced by a wh-word or a wh-phrase. Indirect"
					+ " questions and relative clauses should be bracketed as SBAR, not SBARQ."),

	/**
	 * Inverted declarative sentence.
	 */
	SINV("SINV", GrammaticalStructureLevel.CLAUSE,
			"Inverted declarative sentence, i.e. one in which the subject follows"
					+ " the tensed verb or modal."),

	/**
	 * Inverted yes/no question.
	 */
	SQ("SQ", GrammaticalStructureLevel.CLAUSE,
			"Inverted yes/no question, or main clause of a wh-question, following"
					+ " the wh-phrase in SBARQ."),

	// Phrase Level Tags

	/**
	 * Adjective Phrase.
	 */
	ADJP("ADJP", GrammaticalStructureLevel.PHRASE, "Adjective Phrase."),

	/**
	 * Adverb Phrase.
	 */
	ADVP("ADVP", GrammaticalStructureLevel.PHRASE, "Adverb Phrase."),

	/**
	 * Conjunction Phrase.
	 */
	CONJP("CONJP", GrammaticalStructureLevel.PHRASE, "Conjunction Phrase."),

	/**
	 * Fragment.
	 */
	FRAG("FRAG", GrammaticalStructureLevel.PHRASE, "Fragment."),

	/**
	 * Interjection.
	 * 
	 * @see tag UH
	 */
	INTJ("INTJ", GrammaticalStructureLevel.PHRASE,
			"Interjection. Corresponds approximately to the part-of-speech tag UH."),

	/**
	 * List marker.
	 */
	LST("LST", GrammaticalStructureLevel.PHRASE, "List marker. Includes surrounding punctuation."),

	/**
	 * Not a Constituent.
	 */
	NAC("NAC", GrammaticalStructureLevel.PHRASE,
			"Not a Constituent; used to show the scope of certain prenominal"
					+ " modifiers within an NP."),

	/**
	 * Noun Phrase.
	 */
	NP("NP", GrammaticalStructureLevel.PHRASE, "Noun Phrase."),

	/**
	 * Used within certain complex NPs to mark the head of the NP.
	 */
	NX("NX", GrammaticalStructureLevel.PHRASE,
			"Used within certain complex NPs to mark the head of the NP."
					+ " Corresponds very roughly to N-bar level but used quite differently."),

	/**
	 * Prepositional Phrase.
	 */
	PP("PP", GrammaticalStructureLevel.PHRASE, "Prepositional Phrase."),

	/**
	 * Parenthetical.
	 */
	PRN("PRN", GrammaticalStructureLevel.PHRASE, "Parenthetical."),

	/**
	 * Particle. like a prepositional phrase ex: John went up the ladder. parse:
	 * (S (NP (NNP John)) (VP (VBD went) (PRT (RP up)) (NP (DT the) (NN
	 * ladder)))(. .))
	 */
	PRT("PRT", GrammaticalStructureLevel.PHRASE,
			"Particle. Category for words that should be tagged RP."),

	/**
	 * Quantifier Phrase
	 */
	QP("QP", GrammaticalStructureLevel.PHRASE,
			"Quantifier Phrase (i.e. complex measure/amount phrase); used within NP."),

	/**
	 * Reduced Relative Clause.
	 */
	RRC("RRC", GrammaticalStructureLevel.PHRASE, "Reduced Relative Clause."),

	/**
	 * Unlike Coordinated Phrase.
	 */
	UCP("UCP", GrammaticalStructureLevel.PHRASE, "Unlike Coordinated Phrase."),

	/**
	 * Vereb Phrase.
	 */
	VP("VP", GrammaticalStructureLevel.PHRASE, "Vereb Phrase."),

	/**
	 * Wh-adjective Phrase.
	 */
	WHADJP("WHADJP", GrammaticalStructureLevel.PHRASE,
			"Wh-adjective Phrase. Adjectival phrase containing a wh-adverb, as in how hot."),

	/**
	 * Wh-adverb Phrase.
	 */
	WHAVP("WHAVP", GrammaticalStructureLevel.PHRASE,
			"Wh-adverb Phrase. Introduces a clause with an NP gap. May be null"
					+ " (containing the 0 complementizer) or lexical, containing a wh-adverb"
					+ " such as how or why."),

	/**
	 * Wh-noun Phrase.
	 */
	WHNP("WHNP", GrammaticalStructureLevel.PHRASE,
			"Wh-noun Phrase. Introduces a clause with an NP gap. May be null"
					+ " (containing the 0 complementizer) or lexical, containing some wh-word,"
					+ " e.g. who, which book, whose daughter, none of which, or how many leopards."),

	/**
	 * Wh-prepositional Phrase.
	 */
	WHPP(
			"WHPP",
			GrammaticalStructureLevel.PHRASE,
			"Wh-prepositional Phrase. Prepositional phrase containing a wh-noun phrase"
					+ " (such as of which or by whose authority) that either introduces a PP gap or"
					+ " is contained by a WHNP."),

	/**
	 * Unknown, uncertain, or unbracketable.
	 */
	X("X", GrammaticalStructureLevel.PHRASE,
			"Unknown, uncertain, or unbracketable. X is often used for bracketing typos"
					+ " and in bracketing the...the-constructions."),

	// Word level
	/**
	 * Coordinating conjunction.
	 */
	CC("CC", GrammaticalStructureLevel.WORD, PartOfSpeech.CONJUNCTION, "Coordinating conjunction"),

	/**
	 * Cardinal number.
	 */
	CD("CD", GrammaticalStructureLevel.WORD, PartOfSpeech.NUMBER, "Cardinal number"),

	/**
	 * Determiner.
	 */
	DT("DT", GrammaticalStructureLevel.WORD, PartOfSpeech.DETERMINER, "Determiner"),

	/**
	 * Existential there. There is a fly in my soup. (S (NP (EX There))(VP (VBZ
	 * is)(NP (NP (DT a) (NN fly))(PP (IN in)(NP (PRP$ my) (NN soup)))))(. .))
	 */
	EX("EX", GrammaticalStructureLevel.WORD, PartOfSpeech.MODAL, "Existential there"),

	/**
	 * Foreign word.
	 */
	FW("FW", GrammaticalStructureLevel.WORD, PartOfSpeech.UNKNOWN, "Foreign word"),

	/**
	 * Preposition or subordinating conjunction.
	 */
	IN("IN", GrammaticalStructureLevel.WORD, PartOfSpeech.PREPOSITION,
			"Preposition or subordinating conjunction"),

	/**
	 * Adjective.
	 */
	JJ("JJ", GrammaticalStructureLevel.WORD, PartOfSpeech.ADJECTIVE, "Adjective"),

	/**
	 * Adjective, comparative
	 */
	JJR("JJR", GrammaticalStructureLevel.WORD, PartOfSpeech.ADJECTIVE, "Adjective, comparative"),

	/**
	 * Adjective, superlative
	 */
	JJS("JJS", GrammaticalStructureLevel.WORD, PartOfSpeech.ADJECTIVE, "Adjective, superlative"),

	/**
	 * List item marker
	 */
	LS("LS", GrammaticalStructureLevel.WORD, PartOfSpeech.UNKNOWN, "List item marker"),

	/**
	 * Modal
	 */
	MD("MD", GrammaticalStructureLevel.WORD, PartOfSpeech.MODAL, "Modal"),

	/**
	 * Noun, singular or mass
	 */
	NN("NN", GrammaticalStructureLevel.WORD, PartOfSpeech.NOUN, "Noun, singular or mass"),

	/**
	 * Noun, plural
	 */
	NNS("NNS", GrammaticalStructureLevel.WORD, PartOfSpeech.NOUN, "Noun, plural"),

	/**
	 * Proper noun, singular
	 */
	NNP("NNP", GrammaticalStructureLevel.WORD, PartOfSpeech.NOUN, "Proper noun, singular"),

	/**
	 * Proper noun, plural
	 */
	NNPS("NNPS", GrammaticalStructureLevel.WORD, PartOfSpeech.NOUN, "Proper noun, plural"),

	/**
	 * Predeterminer
	 */
	PDT("PDT", GrammaticalStructureLevel.WORD, PartOfSpeech.DETERMINER, "Predeterminer"),

	/**
	 * Possessive ending for example "John's hair is brown."<br>
	 * (S (NP (NP (NNP John)(POS 's))(NN hair))(VP (VBZ is)(ADJP (JJ brown)))(.
	 * .))
	 */
	POS("POS", GrammaticalStructureLevel.WORD, PartOfSpeech.UNKNOWN, "Possessive ending"),

	/**
	 * Personal pronoun
	 */
	PRP("PRP", GrammaticalStructureLevel.WORD, PartOfSpeech.PRONOUN, "Personal pronoun"),

	/**
	 * Possessive pronoun
	 */
	PRP$("PRP$", GrammaticalStructureLevel.WORD, PartOfSpeech.PRONOUN, "Possessive pronoun"),

	/**
	 * Adverb
	 */
	RB("RB", GrammaticalStructureLevel.WORD, PartOfSpeech.ADVERB, "Adverb"),

	/**
	 * Adverb, comparative list more or quicker
	 */
	RBR("RBR", GrammaticalStructureLevel.WORD, PartOfSpeech.ADVERB, "Adverb, comparative"),

	/**
	 * Adverb, superlative like most or quickest
	 */
	RBS("RBS", GrammaticalStructureLevel.WORD, PartOfSpeech.ADVERB, "Adverb, superlative"),

	/**
	 * Particle - like an adverb/preposition John went up the ladder. "how did
	 * John go on the ladder? up" (S (NP (NNP John))(VP (VBD went)(PRT (RP
	 * up))(NP (DT the) (NN ladder)))(. .)) vs. John went under the ladder.
	 * "where did John go? under the ladder" (S (NP (NNP John))(VP (VBD went)(PP
	 * (IN under)(NP (DT the) (NN ladder))))(. .))
	 */
	RP("RP", GrammaticalStructureLevel.WORD, PartOfSpeech.ADVERB, "Particle"),

	/**
	 * Symbol
	 */
	SYM("SYM", GrammaticalStructureLevel.WORD, PartOfSpeech.SYMBOL, "Symbol"),

	/**
	 * to - like a particle, may act as an adverb or preposition, can be
	 * determined by the phrase. John went to the store. (S (NP (NNP John))(VP
	 * (VBD went)(PP (TO to)(NP (DT the) (NN store))))(. .)) John went under the
	 * bridge. (S (NP (NNP John))(VP (VBD went)(PP (IN under)(NP (DT the) (NN
	 * bridge))))(. .)) the boat swayed to and fro. (S(NP (DT the) (NN boat))(VP
	 * (VBN swayed)(ADVP (TO to)(CC and)(RB fro)))(. .)) the boat sailed to the
	 * reef. (S (NP (DT the)(NN boat))(VP (VBD sailed)(PP (TO to)(NP (DT the)
	 * (NN reef))))(. .))
	 */
	TO("TO", GrammaticalStructureLevel.WORD, PartOfSpeech.PREPOSITION, "to"),

	/**
	 * Interjection
	 */
	UH("UH", GrammaticalStructureLevel.WORD, PartOfSpeech.INTERJECTION, "Interjection"),

	/**
	 * Verb, base form
	 */
	VB("VB", GrammaticalStructureLevel.WORD, PartOfSpeech.VERB, "Verb, base form"),

	/**
	 * Verb, past tense
	 */
	VBD("VBD", GrammaticalStructureLevel.WORD, PartOfSpeech.VERB, "Verb, past tense"),

	/**
	 * Verb, gerund or present participle
	 */
	VBG("VBG", GrammaticalStructureLevel.WORD, PartOfSpeech.VERB,
			"Verb, gerund or present participle"),

	/**
	 * Verb, past participle
	 */
	VBN("VBN", GrammaticalStructureLevel.WORD, PartOfSpeech.VERB, "Verb, past participle"),

	/**
	 * Verb, non-3rd person singular present
	 */
	VBP("VBP", GrammaticalStructureLevel.WORD, PartOfSpeech.VERB,
			"Verb, non-3rd person singular present"),

	/**
	 * Verb, 3rd person singular present
	 */
	VBZ("VBZ", GrammaticalStructureLevel.WORD, PartOfSpeech.VERB,
			"Verb, 3rd person singular present"),

	/**
	 * Wh-determiner
	 */
	WDT("WDT", GrammaticalStructureLevel.WORD, PartOfSpeech.DETERMINER, "Wh-determiner"),

	/**
	 * Wh-pronoun
	 */
	WP("WP", GrammaticalStructureLevel.WORD, PartOfSpeech.PRONOUN, "Wh-pronoun"),

	/**
	 * Possessive wh-pronoun
	 */
	WP$("WP$", GrammaticalStructureLevel.WORD, PartOfSpeech.PRONOUN, "Possessive wh-pronoun"),

	/**
	 * Wh-adverb
	 */
	WRB("WRB", GrammaticalStructureLevel.WORD, PartOfSpeech.ADVERB, "Wh-adverb"),

	/**
	 * 
	 */
	PUNC_DOLLAR("$", GrammaticalStructureLevel.WORD, PartOfSpeech.PUNCTUATION, "A dollar sign ($)."),

	/**
	 * Marks the end of a sentance
	 */
	PUNC_TERMINATOR(".", GrammaticalStructureLevel.WORD, PartOfSpeech.PUNCTUATION,
			"Punctuation ending a sentence such as a period, question mark or exclamation mark."),

	/**
	 * 
	 */
	PUNC_DQUOTE("\"", GrammaticalStructureLevel.WORD, PartOfSpeech.PUNCTUATION,
			"A double quote (\")."),
	/**
	 * 
	 */
	PUNC_SQUOTE("'", GrammaticalStructureLevel.WORD, PartOfSpeech.PUNCTUATION,
			"A single quote (')."),
	/**
	 * 		
	 */
	PUNC_NON_TERMINATOR(",", GrammaticalStructureLevel.WORD, PartOfSpeech.PUNCTUATION,
			"Punctuation joining clauses or phrases such as a comma or semicolon.");

	private static final Map<String, ParseTag> tagsByText = new HashMap<String, ParseTag>();
	static {
		for (ParseTag tag : values()) {
			tagsByText.put(tag.getText(), tag);
		}
	}

	/**
	 * @param text
	 * @return The tag enum based on the parser tag text such as "S", "NP",
	 *         "NN", etc.
	 */
	public static ParseTag tagOf(String text) {
		if (tagsByText.containsKey(text)) {
			return tagsByText.get(text);
		}
		return X;
	}

	private final String text;
	private final String description;
	private final GrammaticalStructureLevel grammaticalStructureLevel;
	private final PartOfSpeech partOfSpeech;

	private ParseTag(String text, GrammaticalStructureLevel grammaticalStructureLevel,
			String description) {
		this.text = text;
		this.grammaticalStructureLevel = grammaticalStructureLevel;
		this.partOfSpeech = PartOfSpeech.UNKNOWN;
		this.description = description;
	}

	private ParseTag(String text, GrammaticalStructureLevel grammaticalStructureLevel,
			PartOfSpeech partOfSpeech, String description) {
		this.text = text;
		this.grammaticalStructureLevel = grammaticalStructureLevel;
		this.partOfSpeech = partOfSpeech;
		this.description = description;
	}

	/**
	 * @return The text of the tag returned by the parser, for example "S",
	 *         "NP", "NNS"
	 */
	public String getText() {
		return text;
	}

	/**
	 * @return An informative descrption for the tag
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return The grammatical level of the structure represented by the tag.
	 */
	public GrammaticalStructureLevel getGrammaticalStructureLevel() {
		return grammaticalStructureLevel;
	}

	/**
	 * @return The part of speech for word level tags, unknown for other levels.
	 */
	public PartOfSpeech getPartOfSpeech() {
		return partOfSpeech;
	}
}
