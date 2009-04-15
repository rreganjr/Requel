/*
 * $Id: Dictionary.java,v 1.1 2008/12/13 00:40:37 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * The Dictionary class is a transient wrapper for importing and exporting the
 * wordnet synsets, senses, words, and categories to and from the dictionary.xml
 * file.<br>
 * NOTE: Importing and Exporting this is very slow because there is so much
 * data. Using sql is at least 100 times faster.
 * 
 * @author ron
 */
@XmlRootElement(name = "dictionary")
public class Dictionary {

	private final NavigableSet<Word> words = new TreeSet<Word>();
	private final NavigableSet<Sense> senses = new TreeSet<Sense>();
	private final NavigableSet<Category> categories = new TreeSet<Category>();
	private final NavigableSet<Synset> synsets = new TreeSet<Synset>();
	private final NavigableSet<SemlinkrefId> semlinkrefs = new TreeSet<SemlinkrefId>();

	public Dictionary() {
	}

	@XmlAttribute(name = "firstWord")
	public String getFirstWord() {
		return words.first().getLemma();
	}

	@XmlAttribute(name = "lastWord")
	public String getLastWord() {
		return words.last().getLemma();
	}

	@XmlElementWrapper(name = "words")
	@XmlElementRef(name = "word")
	public NavigableSet<Word> getWords() {
		return words;
	}

	public void setWords(Collection<Word> words) {
		this.words.addAll(words);
	}

	public NavigableSet<Sense> getSenses() {
		return senses;
	}

	public void setSenses(Collection<Sense> senses) {
		this.senses.addAll(senses);
	}

	@XmlElementWrapper(name = "categories")
	@XmlElementRef(name = "category")
	public NavigableSet<Category> getCategories() {
		return categories;
	}

	public void setCategories(Collection<Category> categories) {
		this.categories.addAll(categories);
	}

	@XmlElementWrapper(name = "synsets")
	@XmlElementRef(name = "synset")
	public NavigableSet<Synset> getSynsets() {
		return synsets;
	}

	public void setSynsets(Collection<Synset> synsets) {
		this.synsets.addAll(synsets);
	}

	public void setSemlinkrefs(Collection<SemlinkrefId> semlinkrefs) {
		this.semlinkrefs.addAll(semlinkrefs);
	}

	public NavigableSet<SemlinkrefId> getSemlinkrefs() {
		return semlinkrefs;
	}
}
