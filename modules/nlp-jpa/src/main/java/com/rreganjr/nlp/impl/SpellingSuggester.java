/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.rreganjr.nlp.dictionary.impl.NLPTextImpl;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.dictionary.GrammaticalStructureLevel;
import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.PartOfSpeech;
import com.rreganjr.nlp.dictionary.DictionaryRepository;

/**
 * Find spelling suggestions
 * 
 * @author ron
 */
@Component("spellingSuggester")
public class SpellingSuggester implements NLPProcessor<Collection<NLPText>> {
	private static final Logger log = Logger.getLogger(SpellingSuggester.class);

	private final DictionaryRepository dictionaryRepository;

	private Long projectId;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public SpellingSuggester(DictionaryRepository dictionaryRepository) {
		this.dictionaryRepository = dictionaryRepository;
	}

	/**
	 * The project whose own dictionary words may appear among the suggestions, in addition to the
	 * two installation-wide layers (issue #313). Null means the installation-wide layers alone.
	 * <p>
	 * Set by {@code NLPProcessorFactory.getSimilarWordFinder(Long)} on a freshly created instance
	 * — this is a prototype bean, so it is never shared between callers.
	 */
	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	protected Long getProjectId() {
		return projectId;
	}

	@Override
	public Collection<NLPText> process(NLPText text) {
		if (text.is(GrammaticalStructureLevel.WORD) && text.hasText()
				&& !text.is(PartOfSpeech.PUNCTUATION)) {
			List<NLPText> results = new ArrayList<NLPText>();
			for (String word : dictionaryRepository.findSpellingSuggestions(projectId,
					text.getText(), 2)) {
				results.add(new NLPTextImpl(word, GrammaticalStructureLevel.WORD));
			}
			return results;
		}
		return Collections.EMPTY_LIST;
	}
}
