/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.nlp;

import java.util.Collection;
import java.util.List;

import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.impl.NLPTextImpl;

/**
 * No-op {@link NLPProcessorFactory} used when NLP is disabled
 * ({@code requel.nlp.enabled=false}). Every method returns a <em>safe empty value</em>
 * rather than {@code null}, so callers (the legacy lexical assistants and the
 * assistant SPI adapters) degrade to "no findings" instead of NPE-ing. This is the
 * single source of truth for the "NLP disabled" contract referenced by the assistant
 * SPI plan (an assistant analysing under it produces an empty {@code AssistantResult}).
 *
 * <p>
 * Behaviour:
 * <ul>
 * <li>Text methods return an empty {@link NLPTextImpl} (no children / leaves), so any
 * token / POS / lemma walk is a no-op.</li>
 * <li>{@code NLPText}-typed processors return their input unchanged.</li>
 * <li>Collection processors (noun phrases, similar / more-specific words) return an
 * empty list — no suggestions.</li>
 * <li>The spelling checker returns {@code TRUE} ("correctly spelled"), which is the
 * value the lexical spelling assistant treats as "no issue"; returning {@code FALSE}
 * would mark every word misspelled. This is the one place the generic
 * "{@code FALSE} for Boolean" default is deliberately inverted to honour the
 * "no findings" contract.</li>
 * <li>The constituent-tree depth finder returns {@code 0} (below any complexity
 * threshold → no complexity issue); String printers return {@code ""}.</li>
 * </ul>
 */
public class NoOpNLPProcessorFactory implements NLPProcessorFactory {

	private static NLPProcessor<NLPText> identity() {
		return text -> text;
	}

	@Override
	public NLPText createNLPText(String text) {
		return new NLPTextImpl(text);
	}

	@Override
	public NLPText processText(String text) {
		return new NLPTextImpl(text);
	}

	@Override
	public NLPText appendText(NLPText... texts) {
		return new NLPTextImpl();
	}

	@Override
	public NLPText appendText(List<NLPText> texts) {
		return new NLPTextImpl();
	}

	@Override
	public NLPProcessor<String> getConstituentTreePrinter() {
		return text -> "";
	}

	@Override
	public NLPProcessor<String> getDependencyPrinter() {
		return text -> "";
	}

	@Override
	public NLPProcessor<String> getSemanticRolePrinter() {
		return text -> "";
	}

	@Override
	public NLPProcessor<Integer> getConstituentTreeDepthFinder() {
		return text -> 0;
	}

	@Override
	public NLPProcessor<NLPText> getLemmatizer() {
		return identity();
	}

	@Override
	public NLPProcessor<NLPText> getParser() {
		return identity();
	}

	@Override
	public NLPProcessor<NLPText> getSentencizer() {
		return identity();
	}

	@Override
	public NLPProcessor<NLPText> getTokenizer() {
		return identity();
	}

	@Override
	public NLPProcessor<NLPText> getPosTagger() {
		return identity();
	}

	@Override
	public NLPProcessor<NLPText> getSemanticRoleLabeler() {
		return identity();
	}

	@Override
	public NLPProcessor<NLPText> getPrimaryVerbFinder() {
		return identity();
	}

	@Override
	public NLPProcessor<NLPText> getWordSenseDisambiguator() {
		return identity();
	}

	@Override
	public NLPProcessor<NLPText> getDictionizer() {
		return identity();
	}

	@Override
	public NLPProcessor<NLPText> getNamedEntityResolver() {
		return identity();
	}

	@Override
	public NLPProcessor<Collection<NLPText>> getNounPhraseFinder() {
		return text -> List.of();
	}

	@Override
	public NLPProcessor<Collection<NLPText>> getSimilarWordFinder() {
		return text -> List.of();
	}

	@Override
	public NLPProcessor<Collection<NLPText>> getMoreSpecificWordSuggester() {
		return text -> List.of();
	}

	@Override
	public NLPProcessor<Boolean> getSpellingChecker() {
		return text -> Boolean.TRUE;
	}
}
