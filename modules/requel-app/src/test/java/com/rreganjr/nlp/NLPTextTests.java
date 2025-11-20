/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
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

import com.rreganjr.TestCase;
import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.impl.ConstituentTreePrinter;
import com.rreganjr.nlp.impl.DependencyPrimaryVerbFinder;
import com.rreganjr.nlp.impl.DependencyPrinter;
import com.rreganjr.nlp.impl.DependencySubjectFinder;
import com.rreganjr.nlp.dictionary.impl.NLPTextImpl;
import com.rreganjr.nlp.impl.Sentencizer;
import com.rreganjr.nlp.impl.StanfordLexicalizedParser;
import com.rreganjr.nlp.impl.StringNLPTextWalker;

public class NLPTextTests extends TestCase {

	private final NLPProcessor<NLPText> parser = new StanfordLexicalizedParser();
	private final NLPProcessor<NLPText> sentencizer = new Sentencizer();
	private final StringNLPTextWalker treePrinter = new StringNLPTextWalker(
			new ConstituentTreePrinter(500, true));
	private final StringNLPTextWalker dependencyPrinter = new StringNLPTextWalker(
			new DependencyPrinter(500));
	private final DependencySubjectFinder subjectFinder = new DependencySubjectFinder();
	private final DependencyPrimaryVerbFinder predicateFinder = new DependencyPrimaryVerbFinder();

	private NLPText process(String sentence) {
		NLPText text = new NLPTextImpl(sentence);
		sentencizer.process(text);
		parser.process(text);
		return text;
	}

	private final TestSentence[] testSentences = new TestSentence[] {
			new TestSentence(
					"new content must be distinguished from archive content with a tag or other visual marker.",
					"new content", "distinguished", "archive content", "tag or other visual marker"),
			new TestSentence("a user searches artists by name or genre.", "a user", "searches",
					"artists", "by name or genre"),
			new TestSentence("a user can search for artists by name or genre.", "a user", "search",
					"artists", "by name or genre"),
			new TestSentence("the system allows a user to search for artists by name or genre.",
					"the system", "allows", "a user", null),
			new TestSentence("artists can be searched by name or genre.", "artists", "searched",
					"by name or genre", null),
			new TestSentence("the system supports searching for artists by name or genre.",
					"the system", "supports", null, null),
			new TestSentence("", null, null, null, null) };

	public void testProcessing() {
		for (TestSentence sentence : testSentences) {
			NLPText text = process(sentence.text);
			printTokensAndLemmas(text);
			System.out.println("Constituent Parse: " + treePrinter.process(text));
			System.out.println("Dependencies: " + dependencyPrinter.process(text));
			NLPText subject = subjectFinder.process(text);
			NLPText predicate = predicateFinder.process(text);
			System.out.println("Subject: " + (subject == null ? "<null>" : subject.getText()));
			System.out
					.println("Predicate: " + (predicate == null ? "<null>" : predicate.getText()));
			assertEquals(sentence.subject, (subject == null ? null : subject.getText()));
			assertEquals(sentence.predicate, (predicate == null ? null : predicate.getText()));

		}
	}

	private void printTokensAndLemmas(NLPText text) {
		StringBuilder sb = new StringBuilder(text.getText().length() * 4);
		for (NLPText word : text.getLeaves()) {
			sb.append("word: ");
			sb.append(word.getText());
			sb.append("[");
			sb.append(word.getPartOfSpeech().name());
			sb.append("]");
			sb.append(" lemma: ");
			sb.append(word.getLemma());
			sb.append("\n");
		}
		System.out.println(sb.toString());
	}

	private static class TestSentence {
		private final String text;
		private final String subject;
		private final String predicate;
		private final String directObject;
		private final String indirectObject;

		protected TestSentence(String text, String subject, String predicate, String directObject,
				String indirectObject) {
			this.text = text;
			this.subject = subject;
			this.predicate = predicate;
			this.directObject = directObject;
			this.indirectObject = indirectObject;
		}
	}
}
