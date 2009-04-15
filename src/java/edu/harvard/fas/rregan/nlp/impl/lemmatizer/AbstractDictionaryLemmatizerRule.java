/*
 * $Id: AbstractDictionaryLemmatizerRule.java,v 1.3 2009/01/26 10:19:00 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.lemmatizer;

import edu.harvard.fas.rregan.nlp.LemmatizerRule;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;

/**
 * Base class for lemmatizers that use the dictionary.
 * 
 * @author ron
 */
public abstract class AbstractDictionaryLemmatizerRule implements LemmatizerRule {

	private final DictionaryRepository dictionaryRepository;

	/**
	 * @param dictionaryRepository
	 */
	public AbstractDictionaryLemmatizerRule(DictionaryRepository dictionaryRepository) {
		this.dictionaryRepository = dictionaryRepository;
	}

	protected DictionaryRepository getDictionaryRepository() {
		return dictionaryRepository;
	}
}
