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

import java.util.List;
import java.util.Set;

import com.rreganjr.nlp.dictionary.Sense;
import com.rreganjr.nlp.dictionary.Word;
import com.rreganjr.nlp.impl.NLPTextImpl;
import com.rreganjr.nlp.impl.wsd.SenseRelationInfo;

/**
 * Represents various levels of text grammatically from word to full stories.
 * 
 * @author ron
 */
public interface NLPText extends Cloneable {

	/**
	 * In a grammatical relationship a dummy node associated to the root of the sentence.
	 */
	public static final NLPText ROOT = new NLPTextImpl("ROOT");

	/**
	 * @return the parent NLPText element that wholly contains this NLPText
	 *         element.
	 */
	public NLPText getParent();

	/**
	 * @return the ancestor NLPText nodes of this node in the order closest
	 *         first. An empty list is returned for nodes with no parents.
	 */
	public List<NLPText> getAncestors();

	/**
	 * @param ancestor -
	 *            a text node.
	 * @return true if the supplied text node is in the ancestors of this node.
	 */
	public boolean isDescendentOf(NLPText ancestor);

	/**
	 * @return the text that makes up this element, possibly conained in sub
	 *         elements.
	 */
	public String getText();

	/**
	 * Return a substring of the leaves of this node.<br>
	 * For example the following string of 8 leaves:<br>
	 * <code>
	 * these are the leaves of this node .
	 * 
	 * getTextRange(2) -> "the leaves of this node ."
	 * getTextRange(0) -> "these are the leaves of this node ."
	 * </code>
	 * 
	 * @param startIndex -
	 *            the zero based index of the first leaf to include.
	 * @return the string of the leaves starting from the leaf at the specified
	 *         index.
	 */
	public String getTextRange(int startIndex);

	/**
	 * Return a substring of the leaves of this node.<br>
	 * For example the following string of 8 leaves:<br>
	 * <code>
	 * these are the leaves of this node .
	 * 
	 * getTextRange(2,3) -> "the leaves"
	 * getTextRange(0,0) -> "these"
	 * </code>
	 * 
	 * @param startIndex -
	 *            the zero based index of the first leaf to include.
	 * @param endIndex -
	 *            the zero based index of the last leaf to include.
	 * @return the string of the leaves from the leaf at the specified index and
	 *         ending at the specified index inclusive.
	 */
	public String getTextRange(int startIndex, int endIndex);

	/**
	 * @param texts -
	 *            zero or more NLPText objects to append to this text.
	 * @return a new NLPText consisting of (as children) this text appended with
	 *         the supplied text
	 */
	public NLPText appendText(NLPText... texts);

	/**
	 * @param texts -
	 *            zero or more NLPText objects to append to this text.
	 * @return a new NLPText consisting of (as children) this text appended with
	 *         the supplied text
	 */
	public NLPText appendText(List<NLPText> texts);

	/**
	 * @return The Penn Treebank tag for this element.
	 */
	public ParseTag getParseTag();

	/**
	 * @param tag -
	 *            the parse tag
	 * @return true if this element has been parsed and the parse tag matches
	 *         the supplied parse tag.
	 */
	public boolean is(ParseTag tag);

	/**
	 * @param tags -
	 *            the parse tags
	 * @return true if this element has been parsed and the parse tag matches
	 *         one of the supplied parse tags.
	 */
	public boolean in(ParseTag... tags);

	/**
	 * @return The GrammaticalStructureLevel of this element, such as WORD,
	 *         PHRASE, or CLAUSE.
	 * @see GrammaticalStructureLevel
	 */
	public GrammaticalStructureLevel getGrammaticalStructureLevel();

	/**
	 * @param grammaticalStructureLevel
	 * @return true if this node's GrammaticalStructureLevel equals the supplied
	 *         level.
	 */
	public boolean is(GrammaticalStructureLevel grammaticalStructureLevel);

	/**
	 * @param grammaticalStructureLevels
	 * @return true if this node's GrammaticalStructureLevel equals one of the
	 *         supplied levels.
	 */
	public boolean in(GrammaticalStructureLevel... grammaticalStructureLevels);

	/**
	 * @return A list of the children NLPText elements in the order they appear
	 *         in this element.
	 */
	public List<NLPText> getChildren();

	/**
	 * Add a new child element to the end of the children list and make its
	 * parent this object.
	 * 
	 * @param child
	 */
	public void addChild(NLPText child);

	/**
	 * Add a child to this NLPText without removing it from another NLPText.
	 * This is used for adding words from another NLPText to a bag of words
	 * NLPText.
	 * 
	 * @param text
	 */
	public void addRef(NLPText text);

	/**
	 * Remove a child from this object.
	 * 
	 * @param child
	 */
	public void removeChild(NLPText child);

	/**
	 * @return A list of the leaf elements of all sub nodes. If this element is
	 *         a leaf an empty list is returned. The leaves should all be Words
	 */
	public List<NLPText> getLeaves();

	/**
	 * @return true if this node is a leaf node.
	 */
	public boolean isLeaf();

	/**
	 * @return the primary verb of the sentence.
	 */
	public NLPText getPrimaryVerb();

	/**
	 * Set the primary verb of the sentence.
	 * 
	 * @param primaryVerb
	 */
	public void setPrimaryVerb(NLPText primaryVerb);

	/**
	 * @param verb -
	 *            an NLPText representing the verb to get the semantic role for.
	 * @return The semantic role of this NLPText for the supplied verb or null.
	 */
	public SemanticRole getSemanticRole(NLPText verb);

	/**
	 * @return For a WORD, PHRASE or CLAUSE, the verbs that this NLPText
	 *         supports with semantic roles.
	 */
	public Set<NLPText> getSupportedVerbs();

	/**
	 * @param verb -
	 *            an NLPText representing the verb that this node fills the
	 *            semantic role for.
	 * @param semanticRole -
	 *            the semantic role of this NLPText node.
	 */
	public void setSemanticRole(NLPText verb, SemanticRole semanticRole);

	/**
	 * @return the index of a WORD level element in the original text, this
	 *         returns -1 for non word tokens.
	 */
	public Integer getWordIndex();

	/**
	 * @return The part of speech of a WORD level element, this returns UNKNOWN
	 *         for non word elements.
	 */
	public PartOfSpeech getPartOfSpeech();

	/**
	 * @return the lemma (base form) of a WORD level element, returns null for
	 *         non-word elements.
	 */
	public String getLemma();

	/**
	 * Set the lemma for a WORD level element.
	 * 
	 * @param lemma -
	 *            the base form of the word returned by text.
	 */
	public void setLemma(String lemma);

	/**
	 * @return The WordNet word of a WORD level element.
	 */
	public Word getDictionaryWord();

	/**
	 * @param dictionaryWord
	 */
	public void setDictionaryWord(Word dictionaryWord);

	/**
	 * @return The WordNet word sense of a WORD level element.
	 */
	public Sense getDictionaryWordSense();

	/**
	 * @param dictionaryWordSense
	 */
	public void setDictionaryWordSense(Sense dictionaryWordSense);

	/**
	 * @return The information collected by the WSD used to determine the sense
	 *         of the word.
	 */
	public Set<SenseRelationInfo> getDictionaryWordSenseRelationInfo();

	/**
	 * Set the information collected by the WSD used to determine the sense of
	 * the word.
	 * 
	 * @param relationInfo
	 */
	public void setDictionaryWordSenseRelationInfo(Set<SenseRelationInfo> relationInfo);

	/**
	 * @return true if this word is the name of an entity such as a person,
	 *         organization, or location.
	 */
	public boolean isNamedEntity();

	/**
	 * set if this word is the name of an entity such as a person, organization,
	 * or location.
	 * 
	 * @param isNamedEntity
	 */
	public void setNamedEntity(boolean isNamedEntity);

	/**
	 * @param partOfSpeech -
	 *            the part of speech.
	 * @return true if this node is a WORD with the supplied part of speech,
	 *         false otherwise.
	 */
	public boolean is(PartOfSpeech partOfSpeech);

	/**
	 * @param partOfSpeechs -
	 *            list of parts of speech
	 * @return true if the part of speech of this text is in the supplied list
	 *         of parts of speech.
	 */
	public boolean in(PartOfSpeech... partOfSpeechs);

	/**
	 * @return
	 */
	public Set<GrammaticalRelation> getGrammaticalRelations();

	/**
	 * @return The grammatical relations this node is the governor of (relation
	 *         from this element) for WORD nodes.
	 */
	public Set<GrammaticalRelation> getGovernorOf();

	/**
	 * @param relation -
	 *            the type of relation.
	 * @return The grammatical relations of the supplied type that this node is
	 *         the governor of for WORD nodes.
	 */
	public Set<GrammaticalRelation> getGovernorOfType(GrammaticalRelationType relation);

	/**
	 * @param relationType -
	 *            the type of relation this word governs.
	 * @param dependent -
	 *            the dependent word of the relation.
	 * @return true if this word is the governor in a relation of the supplied
	 *         type to the supplied word.
	 */
	public boolean isGovernorOf(GrammaticalRelationType relationType, NLPText dependent);

	/**
	 * @return The grammatical relations this node is the dependent of (relation
	 *         to this element) for WORD nodes.
	 */
	public Set<GrammaticalRelation> getDependentOf();

	/**
	 * @param relation -
	 *            the type of relation.
	 * @return The grammatical relations of the supplied type that this node is
	 *         the dependent of for WORD nodes
	 */
	public Set<GrammaticalRelation> getDependentOfType(GrammaticalRelationType relation);

	/**
	 * @param relationType -
	 *            the type of relation this word is a dependent of.
	 * @param governor -
	 *            the governing word of the relation.
	 * @return true if this word is the dependent in a relation of the supplied
	 *         type to the supplied word.
	 */
	public boolean isDependentOf(GrammaticalRelationType relationType, NLPText governor);

	/**
	 * @return true if the NLPText is not empty.
	 */
	public boolean hasText();

	/**
	 * @return true if a lexical analyzer detected errors in the word level
	 *         properties of a WORD, or if any WORD elements of a large element
	 *         has lexical errors.
	 */
	public boolean hasLexicalErrors();

	/**
	 * @return a deep copy of the original object.
	 * @throws CloneNotSupportedException
	 */
	public NLPText clone();

}
