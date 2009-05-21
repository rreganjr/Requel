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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.harvard.fas.rregan.nlp.GrammaticalRelation;
import edu.harvard.fas.rregan.nlp.GrammaticalRelationType;
import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.ParseTag;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.SemanticRole;
import edu.harvard.fas.rregan.nlp.dictionary.Sense;
import edu.harvard.fas.rregan.nlp.dictionary.Word;
import edu.harvard.fas.rregan.nlp.impl.wsd.SenseRelationInfo;

/**
 * Represents various levels of text grammatically from word to full stories.
 * 
 * @author ron
 */
public class NLPTextImpl implements NLPText, Comparable<NLPText> {
	private NLPText parent;
	private String text;
	private GrammaticalStructureLevel grammaticalStructureLevel = GrammaticalStructureLevel.UNKNOWN;
	private ParseTag parseTag = ParseTag.X;
	private final List<NLPText> children = new ArrayList<NLPText>();
	private final Set<GrammaticalRelation> grammaticalRelations = new HashSet<GrammaticalRelation>();
	private NLPText primaryVerb;
	private final Map<NLPText, SemanticRole> semanticRoleVerbMap = new HashMap<NLPText, SemanticRole>();

	// word and lexical info
	private PartOfSpeech partOfSpeech = PartOfSpeech.UNKNOWN;
	private int wordIndex = -1;
	private String lemma;
	private boolean lexicalErrors;
	private Word dictionaryWord;
	private Sense dictionaryWordSense;
	private Set<SenseRelationInfo> dictionaryWordSenseRelationInfo;
	private boolean isNamedEntity;

	// dependency relations
	private final Set<GrammaticalRelation> governs = new TreeSet<GrammaticalRelation>();
	private final Set<GrammaticalRelation> depends = new TreeSet<GrammaticalRelation>();

	/**
	 * @param text
	 */
	public NLPTextImpl(String text) {
		setText(text);
	}

	/**
	 * For creating a bag of words
	 */
	public NLPTextImpl() {
		setGrammaticalStructureLevel(GrammaticalStructureLevel.BAGOFWORDS);
	}

	/**
	 * For creating text of sentence and higher level root nodes with no parent.
	 * 
	 * @param text
	 * @param grammaticalStructureLevel
	 */
	public NLPTextImpl(String text, GrammaticalStructureLevel grammaticalStructureLevel) {
		setText(text);
		setGrammaticalStructureLevel(grammaticalStructureLevel);
	}

	/**
	 * For creating text of sentence and higher level nodes that have a parent.
	 * 
	 * @param parent
	 * @param text
	 * @param grammaticalStructureLevel
	 */
	public NLPTextImpl(NLPText parent, String text,
			GrammaticalStructureLevel grammaticalStructureLevel) {
		setParent(parent);
		setText(text);
		setGrammaticalStructureLevel(grammaticalStructureLevel);
	}

	/**
	 * For creating sentence and lower level nodes based on a parse tag.
	 * 
	 * @param parent
	 * @param parseTag
	 */
	protected NLPTextImpl(NLPText parent, ParseTag parseTag) {
		setParent(parent);
		setParseTag(parseTag);
	}

	/**
	 * TODO: this is public for some old tests.
	 * 
	 * @param text
	 * @param partOfSpeech
	 */
	public NLPTextImpl(String text, PartOfSpeech partOfSpeech) {
		setText(text);
		setPartOfSpeech(partOfSpeech);
		setGrammaticalStructureLevel(GrammaticalStructureLevel.WORD);
	}

	// for word level nodes
	protected NLPTextImpl(NLPText parent, int wordIndex, String text, ParseTag parseTag) {
		setParent(parent);
		setWordIndex(wordIndex);
		setText(text);
		setParseTag(parseTag);
	}

	public NLPText getParent() {
		return parent;
	}

	protected void setParent(NLPText parent) {
		this.parent = parent;
	}

	@Override
	public List<NLPText> getAncestors() {
		List<NLPText> ancestors = new ArrayList<NLPText>(10);
		NLPText current = getParent();
		while (current != null) {
			ancestors.add(current);
			current = current.getParent();
		}
		return ancestors;
	}

	@Override
	public boolean isDescendentOf(NLPText ancestor) {
		return getAncestors().contains(ancestor);
	}

	public String getText() {
		if ((text == null) && !getChildren().isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append(getChildren().get(0).getText());
			for (int i = 1; i < getChildren().size(); i++) {
				// don't add a space before a posessive "'s" or punctuation
				NLPText next = getChildren().get(i);
				if (!next.is(ParseTag.POS) && !next.is(PartOfSpeech.PUNCTUATION)) {
					sb.append(" ");
				}
				sb.append(next.getText());
			}
			text = sb.toString();
		}
		return text;
	}

	@Override
	public String getTextRange(int startIndex, int endIndex) {
		StringBuilder sb = new StringBuilder();
		sb.append(getLeaves().get(startIndex).getText());
		for (int i = startIndex + 1; i <= endIndex; i++) {
			NLPText next = getLeaves().get(i);
			// don't add a space before a posessive "'s" or punctuation
			if (!next.is(ParseTag.POS) && !next.is(PartOfSpeech.PUNCTUATION)) {
				sb.append(" ");
			}
			sb.append(next.getText());
		}
		return sb.toString();
	}

	@Override
	public String getTextRange(int startIndex) {
		return getTextRange(startIndex, getLeaves().size() - 1);
	}

	protected void setText(String text) {
		this.text = text;
	}

	public ParseTag getParseTag() {
		return parseTag;
	}

	public boolean is(ParseTag tag) {
		return getParseTag().equals(tag);
	}

	@Override
	public boolean in(ParseTag... tags) {
		for (ParseTag tag : tags) {
			if (is(tag)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean in(GrammaticalStructureLevel... grammaticalStructureLevels) {
		for (GrammaticalStructureLevel gsl : grammaticalStructureLevels) {
			if (is(gsl)) {
				return true;
			}
		}
		return false;
	}

	public void setParseTag(ParseTag parseTag) {
		this.parseTag = parseTag;
		if (parseTag.getGrammaticalStructureLevel() != null) {
			setGrammaticalStructureLevel(parseTag.getGrammaticalStructureLevel());
		}
		if (parseTag.getPartOfSpeech() != null) {
			setPartOfSpeech(parseTag.getPartOfSpeech());
		}
	}

	public GrammaticalStructureLevel getGrammaticalStructureLevel() {
		return grammaticalStructureLevel;
	}

	public boolean is(GrammaticalStructureLevel grammaticalStructureLevel) {
		return getGrammaticalStructureLevel().equals(grammaticalStructureLevel);
	}

	protected void setGrammaticalStructureLevel(GrammaticalStructureLevel grammaticalStructureLevel) {
		this.grammaticalStructureLevel = grammaticalStructureLevel;
	}

	public List<NLPText> getChildren() {
		return children;
	}

	@Override
	public NLPText appendText(List<NLPText> texts) {
		NLPText newText = new NLPTextImpl(null);
		newText.addChild(this);
		if (texts != null) {
			for (NLPText t : texts) {
				newText.addChild(t.clone());
			}
		}
		return newText;
	}

	@Override
	public NLPText appendText(NLPText... texts) {
		return appendText(Arrays.asList(texts));
	}

	@Override
	public void addChild(NLPText child) {
		if (child.getParent() != null) {
			child.getParent().removeChild(child);
		}
		((NLPTextImpl) child).setParent(this);
		getChildren().add(child);
	}

	public void addRef(NLPText text) {
		getChildren().add(text);
	}

	@Override
	public void removeChild(NLPText child) {
		getChildren().remove(child);
	}

	public List<NLPText> getLeaves() {
		List<NLPText> leaves = new ArrayList<NLPText>();
		addLeaves(leaves);
		return leaves;
	}

	public boolean isLeaf() {
		return getChildren().size() == 0;
	}

	private void addLeaves(final List<NLPText> leaves) {
		for (NLPText child : getChildren()) {
			if (child.isLeaf()) {
				leaves.add(child);
			} else {
				((NLPTextImpl) child).addLeaves(leaves);
			}
		}
	}

	@Override
	public Set<GrammaticalRelation> getGrammaticalRelations() {
		return grammaticalRelations;
	}

	@Override
	public void setPrimaryVerb(NLPText primaryVerb) {
		this.primaryVerb = primaryVerb;
	}

	@Override
	public NLPText getPrimaryVerb() {
		return primaryVerb;
	}

	@Override
	public Set<NLPText> getSupportedVerbs() {
		return semanticRoleVerbMap.keySet();
	}

	@Override
	public SemanticRole getSemanticRole(NLPText verb) {
		return semanticRoleVerbMap.get(verb);
	}

	@Override
	public void setSemanticRole(NLPText verb, SemanticRole semanticRole) {
		semanticRoleVerbMap.put(verb, semanticRole);
	}

	public Integer getWordIndex() {
		return wordIndex;
	}

	protected void setWordIndex(Integer wordIndex) {
		this.wordIndex = wordIndex;
	}

	public PartOfSpeech getPartOfSpeech() {
		return partOfSpeech;
	}

	public boolean is(PartOfSpeech partOfSpeech) {
		return getPartOfSpeech().equals(partOfSpeech);
	}

	public void setPartOfSpeech(PartOfSpeech partOfSpeech) {
		this.partOfSpeech = partOfSpeech;
	}

	@Override
	public boolean in(PartOfSpeech... partOfSpeechs) {
		for (PartOfSpeech partOfSpeech : partOfSpeechs) {
			if (is(partOfSpeech)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Word getDictionaryWord() {
		return dictionaryWord;
	}

	public void setDictionaryWord(Word dictionaryWord) {
		this.dictionaryWord = dictionaryWord;
	}

	@Override
	public Sense getDictionaryWordSense() {
		return dictionaryWordSense;
	}

	public void setDictionaryWordSense(Sense dictionaryWordSense) {
		this.dictionaryWordSense = dictionaryWordSense;
	}

	@Override
	public Set<SenseRelationInfo> getDictionaryWordSenseRelationInfo() {
		return dictionaryWordSenseRelationInfo;
	}

	@Override
	public void setDictionaryWordSenseRelationInfo(Set<SenseRelationInfo> relationInfo) {
		this.dictionaryWordSenseRelationInfo = dictionaryWordSenseRelationInfo;
	}

	@Override
	public boolean isNamedEntity() {
		return isNamedEntity;
	}

	@Override
	public void setNamedEntity(boolean isNamedEntity) {
		this.isNamedEntity = isNamedEntity;
	}

	public boolean hasText() {
		return (getText() != null) && (getText().length() > 0);
	}

	public Set<GrammaticalRelation> getGovernorOf() {
		return governs;
	}

	protected void addGovernorOf(GrammaticalRelation relation) {
		governs.add(relation);
	}

	public Set<GrammaticalRelation> getGovernorOfType(GrammaticalRelationType relation) {
		Set<GrammaticalRelation> relations = new TreeSet<GrammaticalRelation>();
		for (GrammaticalRelation rel : getGovernorOf()) {
			if (rel.getType().isA(relation)) {
				relations.add(rel);
			}
		}
		return relations;
	}

	@Override
	public boolean isGovernorOf(GrammaticalRelationType relationType, NLPText dependent) {
		for (GrammaticalRelation relation : getGovernorOfType(relationType)) {
			if (dependent.equals(relation.getDependent()) && this.equals(relation.getGovernor())) {
				return true;
			}
		}
		return false;
	}

	public Set<GrammaticalRelation> getDependentOf() {
		return depends;
	}

	protected void addDependentOf(GrammaticalRelation relation) {
		depends.add(relation);
	}

	public Set<GrammaticalRelation> getDependentOfType(GrammaticalRelationType relation) {
		Set<GrammaticalRelation> relations = new TreeSet<GrammaticalRelation>();
		for (GrammaticalRelation rel : getDependentOf()) {
			if ((rel != null) && (rel.getType() != null) && rel.getType().isA(relation)) {
				relations.add(rel);
			}
		}
		return relations;
	}

	@Override
	public boolean isDependentOf(GrammaticalRelationType relationType, NLPText governor) {
		for (GrammaticalRelation relation : getDependentOfType(relationType)) {
			if (governor.equals(relation.getGovernor()) && this.equals(relation.getDependent())) {
				return true;
			}
		}
		return false;
	}

	public String getLemma() {
		return (lemma == null ? getText() : lemma);
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public boolean hasLexicalErrors() {
		if (is(GrammaticalStructureLevel.WORD)) {
			return lexicalErrors;
		} else {
			for (NLPText leaf : getLeaves()) {
				return leaf.hasLexicalErrors();
			}
		}
		return false;
	}

	public void setLexicalErrors(boolean lexicalErrors) {
		this.lexicalErrors = lexicalErrors;
	}

	@Override
	public NLPText clone() {
		try {
			return (NLPText) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	@Override
	public int compareTo(NLPText o) {
		int posCompare = getPartOfSpeech().compareTo(o.getPartOfSpeech());
		int textCompare = getText().compareTo(o.getText());
		return (posCompare != 0 ? posCompare : textCompare);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((grammaticalStructureLevel == null) ? 0 : grammaticalStructureLevel.hashCode());
		result = prime * result + ((partOfSpeech == null) ? 0 : partOfSpeech.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + wordIndex;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof NLPText)) {
			return false;
		}
		final NLPText other = (NLPText) obj;
		if (grammaticalStructureLevel == null) {
			if (other.getGrammaticalStructureLevel() != null) {
				return false;
			}
		} else if (!grammaticalStructureLevel.equals(other.getGrammaticalStructureLevel())) {
			return false;
		}
		if (partOfSpeech == null) {
			if (other.getPartOfSpeech() != null) {
				return false;
			}
		} else if (!partOfSpeech.equals(other.getPartOfSpeech())) {
			return false;
		}
		if (text == null) {
			if (other.getText() != null) {
				return false;
			}
		} else if (!text.equals(other.getText())) {
			return false;
		}
		if (wordIndex != other.getWordIndex()) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return getText();
	}
}
