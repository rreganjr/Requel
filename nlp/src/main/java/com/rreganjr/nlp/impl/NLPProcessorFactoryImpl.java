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
package com.rreganjr.nlp.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.NLPProcessor;
import com.rreganjr.nlp.NLPProcessorFactory;
import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.impl.lemmatizer.SimpleLemmatizer;
import com.rreganjr.nlp.impl.srl.SemanticRoleLabeler;
import com.rreganjr.nlp.impl.srl.SemanticRolePrinter;
import com.rreganjr.nlp.impl.wsd.WordnetWSD;

/**
 * This class serves two duties. It acts as a factory for getting processors
 * that perform an NLP task on NLPText, and it acts as a simple controller
 * through the processText() method that orchestrates the primary NLP Tasks.
 * 
 * @author ron
 */
@Component("nlpProcessorFactory")
@Scope("singleton")
public class NLPProcessorFactoryImpl implements NLPProcessorFactory, ApplicationContextAware {
	private static final Log log = LogFactory.getLog(NLPProcessorFactoryImpl.class);

	private ApplicationContext applicationContext;

	// cache of processed text
	private final Map<String, NLPText> processedTextCache = new HashMap<String, NLPText>();

	/**
	 *
	 */
	public NLPProcessorFactoryImpl() {
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * @return
	 */
	protected ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	/**
	 * @see com.rreganjr.nlp.NLPProcessorFactory#createNLPText(String)
	 */
	@Override
	public NLPText createNLPText(String text) {
		return new NLPTextImpl(text);
	}

	@Override
	public NLPText processText(String text) {
		long startTime = System.currentTimeMillis();
		NLPText nlpText = null;
		if (!processedTextCache.containsKey(text)) {
			try {
				nlpText = createNLPText(text);
				getSentencizer().process(nlpText);
				getParser().process(nlpText);
				getPrimaryVerbFinder().process(nlpText);
				getLemmatizer().process(nlpText);
				getDictionizer().process(nlpText);
				getNamedEntityResolver().process(nlpText);
				getWordSenseDisambiguator().process(nlpText);
				getSemanticRoleLabeler().process(nlpText);
				processedTextCache.put(text, nlpText);
			} catch (NLPProcessorException e) {
				log.error("NLP processing threw an exception.", e);
			}
		} else {
			nlpText = processedTextCache.get(text);
		}
		log.info("NLP Processing Time: " + (System.currentTimeMillis() - startTime) + " ms");
		log.info("processed text: \n" + getConstituentTreePrinter().process(nlpText));
		return nlpText;
	}

	@Override
	public NLPText appendText(List<NLPText> texts) {
		NLPText newText = new NLPTextImpl(null);
		if (texts != null) {
			for (NLPText text : texts) {
				newText.addChild(text.clone());
			}
		}
		return newText;
	}

	@Override
	public NLPText appendText(NLPText... texts) {
		return appendText(Arrays.asList(texts));
	}

	@Override
	public NLPProcessor<String> getConstituentTreePrinter() {
		return new StringNLPTextWalker(new ConstituentTreePrinter());
	}

	@Override
	public NLPProcessor<String> getDependencyPrinter() {
		return new StringNLPTextWalker(new DependencyPrinter());
	}

	@Override
	public NLPProcessor<String> getSemanticRolePrinter() {
		return new StringNLPTextWalker(new SemanticRolePrinter());
	}

	@Override
	public NLPProcessor<Integer> getConstituentTreeDepthFinder() {
		return new IntegerNLPTextWalker(new ConstituentTreeDepthFinder());
	}

	/**
	 * @see com.rreganjr.nlp.NLPProcessorFactory#getSentencizer()
	 */
	@Override
	public NLPProcessor<NLPText> getSentencizer() {
		return newInstance(Sentencizer.class);
	}

	/**
	 * @see com.rreganjr.nlp.NLPProcessorFactory#getTokenizer()
	 */
	@Override
	public NLPProcessor<NLPText> getTokenizer() {
		return getParser();
	}

	/**
	 * @see com.rreganjr.nlp.NLPProcessorFactory#getParser()
	 */
	@Override
	public NLPProcessor<NLPText> getParser() {
		return newInstance(StanfordLexicalizedParser.class);
	}

	@Override
	public NLPProcessor<NLPText> getLemmatizer() {
		return newInstance(SimpleLemmatizer.class);
	}

	public NLPProcessor<NLPText> getWordSenseDisambiguator() {
		return newInstance(WordnetWSD.class);
	}

	@Override
	public NLPProcessor<NLPText> getDictionizer() {
		return newInstance(Dictionizer.class);
	}

	@Override
	public NLPProcessor<Collection<NLPText>> getSimilarWordFinder() {
		return newInstance(SpellingSuggester.class);
	}

	@Override
	public NLPProcessor<Collection<NLPText>> getMoreSpecificWordSuggester() {
		return newInstance(MoreSpecificWordSuggester.class);
	}

	@Override
	public NLPProcessor<Boolean> getSpellingChecker() {
		return newInstance(SpellingChecker.class);
	}

	/**
	 * @see com.rreganjr.nlp.NLPProcessorFactory#getSemanticRoleLabeler()
	 */
	@Override
	public NLPProcessor<NLPText> getSemanticRoleLabeler() {
		return newInstance(SemanticRoleLabeler.class);
	}

	@Override
	public NLPProcessor<Collection<NLPText>> getNounPhraseFinder() {
		return newInstance(NounPhraseFinder.class);
	}

	@Override
	public NLPProcessor<NLPText> getNamedEntityResolver() {
		return newInstance(StanfordNameEntityRecognizer.class);
	}

	@Override
	public NLPProcessor<NLPText> getPrimaryVerbFinder() {
		return newInstance(DependencyPrimaryVerbFinder.class);
	}

	protected <T> T newInstance(Class<T> processorType) {
		return (T) getApplicationContext().getAutowireCapableBeanFactory()
				.createBean(processorType);
	}
}
