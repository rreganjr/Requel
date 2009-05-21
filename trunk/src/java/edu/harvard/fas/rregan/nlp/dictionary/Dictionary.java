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
