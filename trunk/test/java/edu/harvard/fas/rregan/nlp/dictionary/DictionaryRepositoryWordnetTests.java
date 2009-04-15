/*
 * $Id: DictionaryRepositoryWordnetTests.java,v 1.5 2009/01/24 11:08:39 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.harvard.fas.rregan.AbstractIntegrationTestCase;
import edu.harvard.fas.rregan.TestCase;
import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.impl.ConstituentTreePrinter;
import edu.harvard.fas.rregan.nlp.impl.DependencyPrinter;
import edu.harvard.fas.rregan.nlp.impl.NLPTextImpl;
import edu.harvard.fas.rregan.nlp.impl.PartOfSpeechAndSensePrinter;
import edu.harvard.fas.rregan.nlp.impl.StringNLPTextWalker;
import edu.harvard.fas.rregan.nlp.impl.wsd.SenseRelationInfo;
import edu.harvard.fas.rregan.nlp.impl.wsd.WordnetWSD;

/**
 * @author ron
 */
public class DictionaryRepositoryWordnetTests extends AbstractIntegrationTestCase {

	private NLPProcessor<NLPText> sentencizer;
	private NLPProcessor<NLPText> parser;
	private NLPProcessor<NLPText> lemmatizer;
	private NLPProcessor<NLPText> dictionizer;
	private NLPProcessor<NLPText> namedEntityResolver;
	private WordnetWSD wordSenseDisambiguator;
	private StringNLPTextWalker senseInfoPrinter;
	private StringNLPTextWalker constituentPrinter;
	private StringNLPTextWalker dependencyPrinter;

	public DictionaryRepositoryWordnetTests() {
	}

	@Override
	protected void onSetUp() throws Exception {
		super.onSetUp();
		sentencizer = getNlpProcessorFactory().getSentencizer();
		parser = getNlpProcessorFactory().getParser();
		lemmatizer = getNlpProcessorFactory().getLemmatizer();
		dictionizer = getNlpProcessorFactory().getDictionizer();
		namedEntityResolver = getNlpProcessorFactory().getNamedEntityResolver();
		wordSenseDisambiguator = new WordnetWSD(getDictionaryRepository());

		senseInfoPrinter = new StringNLPTextWalker(new PartOfSpeechAndSensePrinter(500));
		constituentPrinter = new StringNLPTextWalker(new ConstituentTreePrinter(500, true));
		dependencyPrinter = new StringNLPTextWalker(new DependencyPrinter(500));
	}

	public void testSimilarityCancerCold() throws Exception {
		try {
			assertMostSimilar("cancer", PartOfSpeech.NOUN, 1, "cold", PartOfSpeech.NOUN, 1);
		} catch (Exception e) {
			log.error("exception in test: " + e, e);
			throw e;
		}
	}

	public void testSimilarityBankLibrary() throws Exception {
		try {
			assertMostSimilar("bank", PartOfSpeech.NOUN, 9, "library", PartOfSpeech.NOUN, 3);
		} catch (Exception e) {
			log.error("exception in test: " + e, e);
			throw e;
		}
	}

	public void testSimilarityBankBrae() throws Exception {
		try {
			assertMostSimilar("bank", PartOfSpeech.NOUN, 1, "brae", PartOfSpeech.NOUN, 1);
		} catch (Exception e) {
			log.error("exception in test: " + e, e);
			throw e;
		}
	}

	public void testSimilarityRunWalk() throws Exception {
		try {
			assertMostSimilar("run", PartOfSpeech.NOUN, 7, "walk", PartOfSpeech.NOUN, 1);
		} catch (Exception e) {
			log.error("exception in test: " + e, e);
			throw e;
		}
	}

	public void testSimilarityRunDevelop() throws Exception {
		try {
			assertMostSimilar("run", PartOfSpeech.VERB, 37, "develop", PartOfSpeech.VERB, 18);
		} catch (Exception e) {
			log.error("exception in test: " + e, e);
			throw e;
		}
	}

	public void testDefinitionSimilarityDepositDeposit() throws Exception {
		try {
			assertMostDefinitionSimilarity("deposit", PartOfSpeech.NOUN, 4, "deposit",
					PartOfSpeech.VERB, 2);
		} catch (Exception e) {
			log.error("exception in test: " + e, e);
			throw e;
		}
	}

	public void testRelatednessDepositDeposit() throws Exception {
		try {
			assertMostRelated("deposit", PartOfSpeech.NOUN, 4, "deposit", PartOfSpeech.VERB, 2);
		} catch (Exception e) {
			log.error("exception in test: " + e, e);
			throw e;
		}
	}

	public void testRelatednessDepositN3DepositV2() throws Exception {
		SensePair expectedMostRelated = makeSensePair("deposit", PartOfSpeech.NOUN, 3, "deposit",
				PartOfSpeech.VERB, 2);
		wordSenseDisambiguator.definitionSimilarity(expectedMostRelated.getSense1(),
				expectedMostRelated.getSense2());
	}

	public void testRelated1() throws Exception {
		SensePair pair = makeSensePair("deposit", PartOfSpeech.NOUN, 4, "deposit",
				PartOfSpeech.VERB, 2);
		System.out.println(pair + " -> "
				+ wordSenseDisambiguator.relatedness(pair.getSense1(), pair.getSense2()));
		System.out.println(pair + " -> "
				+ wordSenseDisambiguator.definitionSimilarity(pair.getSense1(), pair.getSense2()));
	}

	public void testRelated2() throws Exception {
		SensePair pair = makeSensePair("deposit", PartOfSpeech.NOUN, 7, "deposit",
				PartOfSpeech.VERB, 2);
		System.out.println(pair + " -> "
				+ wordSenseDisambiguator.relatedness(pair.getSense1(), pair.getSense2()));
		System.out.println(pair + " -> "
				+ wordSenseDisambiguator.definitionSimilarity(pair.getSense1(), pair.getSense2()));
	}

	public void testRelated3() throws Exception {
		SensePair pair = makeSensePair("deposit", PartOfSpeech.NOUN, 7, "deposit",
				PartOfSpeech.VERB, 3);
		System.out.println(pair + " -> "
				+ wordSenseDisambiguator.relatedness(pair.getSense1(), pair.getSense2()));
		System.out.println(pair + " -> "
				+ wordSenseDisambiguator.definitionSimilarity(pair.getSense1(), pair.getSense2()));
	}

	public void testRelated4() throws Exception {
		SensePair pair = makeSensePair("charge", PartOfSpeech.VERB, 12, "purchase",
				PartOfSpeech.NOUN, 1);
		System.out.println(pair + " -> "
				+ wordSenseDisambiguator.relatedness(pair.getSense1(), pair.getSense2()));
		System.out.println(pair + " -> "
				+ wordSenseDisambiguator.definitionSimilarity(pair.getSense1(), pair.getSense2()));
	}

	public void testRelated5() throws Exception {
		SensePair pair = makeSensePair("charge", PartOfSpeech.VERB, 12, "credit",
				PartOfSpeech.NOUN, 5);
		System.out.println(pair + " -> "
				+ wordSenseDisambiguator.relatedness(pair.getSense1(), pair.getSense2()));
		System.out.println(pair + " -> "
				+ wordSenseDisambiguator.definitionSimilarity(pair.getSense1(), pair.getSense2()));
	}

	public void testRelated6() throws Exception {
		SensePair pair = makeSensePair("charge", PartOfSpeech.VERB, 7, "larceny",
				PartOfSpeech.NOUN, 1);
		System.out.println(pair + " -> "
				+ wordSenseDisambiguator.relatedness(pair.getSense1(), pair.getSense2()));
		System.out.println(pair + " -> "
				+ wordSenseDisambiguator.definitionSimilarity(pair.getSense1(), pair.getSense2()));
	}

	public void testDefinitionSimilarityBankDeposit() throws Exception {
		try {
			assertMostDefinitionSimilarity("bank", PartOfSpeech.NOUN, 2, "deposit",
					PartOfSpeech.VERB, 2);
		} catch (Exception e) {
			log.error("exception in test: " + e, e);
			throw e;
		}
	}

	public void testDefinitionSimilarityBillPay() throws Exception {
		try {
			assertMostDefinitionSimilarity("bill", PartOfSpeech.NOUN, 2, "pay", PartOfSpeech.VERB,
					1);
		} catch (Exception e) {
			log.error("exception in test: " + e, e);
			throw e;
		}
	}

	public void testDefinitionSimilarityBillUnconstitutional() throws Exception {
		try {
			// bill 106536853, 13899
			// unconstitutional 300180211, 137459
			assertMostDefinitionSimilarity("bill", PartOfSpeech.NOUN, 1, "unconstitutional",
					PartOfSpeech.ADJECTIVE, 1);
		} catch (Exception e) {
			log.error("exception in test: " + e, e);
			throw e;
		}
	}

	public void testDefinitionSimilarityAmendmentUnconstitutional() throws Exception {
		try {
			// unconstitutional 300180211, 137459
			// amendment 101250101, 4591
			assertMostDefinitionSimilarity("amendment", PartOfSpeech.NOUN, 2, "unconstitutional",
					PartOfSpeech.ADJECTIVE, 1);
		} catch (Exception e) {
			log.error("exception in test: " + e, e);
			throw e;
		}
	}

	public void testColocations() throws Exception {
		try {
			String sentence = "The gadget faker operates or sells his phony machines for 5 to 10000 anything the traffic will bear";
			NLPText text = process(sentence);
			NLPText word = text.getLeaves().get(5);
			NLPText contextWord = text.getLeaves().get(17);
			List<Map<NLPText, Sense>> colocations = wordSenseDisambiguator
					.findSemcorSentenceColocations(word, contextWord);
			assertEquals(1, colocations.size());
			for (Map<NLPText, Sense> wordSenseMap : colocations) {

				if (wordSenseMap.containsKey(word)) {
					assertEquals(1, wordSenseMap.get(word).getRank().intValue());
				} else if (wordSenseMap.containsKey(contextWord)) {
					assertEquals(8, wordSenseMap.get(contextWord).getRank().intValue());
				} else {
					fail("expected sell#1 and bear#8, but found " + wordSenseMap);
				}
			}
		} catch (Exception e) {
			log.error("exception in test: " + e, e);
			throw e;
		}
	}

	public void testDisambiguate() {
		String sentence = "Users will access the Streaming Video service via a link from the Virgin XL WAP environment.";
		String expectedSenseInfo = "user#n#1 will access#v#1 the Streaming#n#1 Video#n#1 service#n#8 via a link#n#3 from the Virgin#n#1 XL#n#1 WAP#n#1 environment#n#2 .";
		NLPText text = process(sentence);
		long start = System.currentTimeMillis();
		wordSenseDisambiguator.process(text);
		log.info("wsd time: " + (System.currentTimeMillis() - start) + " ms");
		log.info(constituentPrinter.process(text));
		log.info(dependencyPrinter.process(text));
		String actualSenseInfo = senseInfoPrinter.process(text);
		log.info(actualSenseInfo);
		for (NLPText word : text.getLeaves()) {
			if (word.getDictionaryWordSense() != null) {
				Synset synset = word.getDictionaryWordSense().getSynset();
				System.out.println(word + "[" + synset.getId() + "]: " + synset.getDefinition());
			} else {
				System.out.println(word);
			}
		}
		TestCase.assertEqualsIgnoreWhitespace(expectedSenseInfo, actualSenseInfo);
	}

	public void testDisambiguate2() {
		String sentence = "The new content must be distinguished from the old content with a tag or other visual marker.";
		String expectedSenseInfo = "The new#a#1 content#n#1 must be#v#4 distinguished#a#1 from the old#a#2 content#n#1 with a tag#n#1 or other#a#1 visual#a#2 marker#n#1 .";
		NLPText text = process(sentence);
		long start = System.currentTimeMillis();
		wordSenseDisambiguator.process(text);
		log.info("wsd time: " + (System.currentTimeMillis() - start) + " ms");
		log.info(constituentPrinter.process(text));
		log.info(dependencyPrinter.process(text));
		String actualSenseInfo = senseInfoPrinter.process(text);
		log.info(actualSenseInfo);
		for (NLPText word : text.getLeaves()) {
			if (word.getDictionaryWordSense() != null) {
				Synset synset = word.getDictionaryWordSense().getSynset();
				log.info(word + "[" + synset.getId() + "]: " + synset.getDefinition());
				log.info(word.getDictionaryWordSenseRelationInfo());
			} else {
				log.info(word);
			}
		}
		TestCase.assertEqualsIgnoreWhitespace(expectedSenseInfo, actualSenseInfo);
	}

	private NLPText process(String sentence) {
		NLPText text = new NLPTextImpl(sentence);
		sentencizer.process(text);
		parser.process(text);
		namedEntityResolver.process(text);
		lemmatizer.process(text);
		dictionizer.process(text);
		return text;
	}

	private void assertMostSimilar(String lemma1, PartOfSpeech pos1, int senseRank1, String lemma2,
			PartOfSpeech pos2, int senseRank2) throws Exception {
		Map<SensePair, SenseRelationInfo> sensePairs = new HashMap<SensePair, SenseRelationInfo>();

		Word word1 = getDictionaryRepository().findWord(lemma1);
		Word word2 = getDictionaryRepository().findWord(lemma2);
		SensePair expectedMostSimilar = new SensePair(word1.getSense(pos1, senseRank1), word2
				.getSense(pos2, senseRank2));
		SenseRelationInfo expectedMaxSimilarity = wordSenseDisambiguator.similarity(
				expectedMostSimilar.getSense1(), expectedMostSimilar.getSense2());

		log.info(expectedMostSimilar + " -> " + expectedMaxSimilarity);
		for (Sense sense1 : word1.getSenses()) {
			for (Sense sense2 : word2.getSenses()) {
				sensePairs.put(new SensePair(sense1, sense2), wordSenseDisambiguator.similarity(
						sense1, sense2));
			}
		}

		SenseRelationInfo maxSimilarity = null;
		SensePair actualMostSimilar = null;
		for (SensePair key : sensePairs.keySet()) {
			SenseRelationInfo similarity = sensePairs.get(key);
			if ((maxSimilarity == null) || (similarity.getRank() > maxSimilarity.getRank())) {
				maxSimilarity = similarity;
				actualMostSimilar = key;
			}
			if (similarity.getRank() > 0.0) {
				log.info(key + " -> " + similarity);
			}
		}
		assertEquals(expectedMostSimilar, actualMostSimilar);
	}

	private SensePair makeSensePair(String lemma1, PartOfSpeech pos1, int senseRank1,
			String lemma2, PartOfSpeech pos2, int senseRank2) {
		Word word1 = getDictionaryRepository().findWord(lemma1);
		Word word2 = getDictionaryRepository().findWord(lemma2);
		return new SensePair(word1.getSense(pos1, senseRank1), word2.getSense(pos2, senseRank2));
	}

	private void assertMostRelated(String lemma1, PartOfSpeech pos1, int senseRank1, String lemma2,
			PartOfSpeech pos2, int senseRank2) throws Exception {
		Map<SensePair, SenseRelationInfo> sensePairs = new HashMap<SensePair, SenseRelationInfo>();
		SensePair expectedMostRelated = makeSensePair(lemma1, pos1, senseRank1, lemma2, pos2,
				senseRank2);
		SenseRelationInfo expectedMaxRelatedness = wordSenseDisambiguator.relatedness(
				expectedMostRelated.getSense1(), expectedMostRelated.getSense2());
		log.info(expectedMostRelated + " -> " + expectedMaxRelatedness);

		Word word1 = expectedMostRelated.getSense1().getWord();
		Word word2 = expectedMostRelated.getSense2().getWord();

		// for relatedness we want the best senses in the expected parts of
		// speech
		for (Sense sense1 : word1.getSenses(pos1)) {
			for (Sense sense2 : word2.getSenses(pos2)) {
				// only compare different parts of speech
				if (!sense1.getSynset().getPartOfSpeech().equals(
						sense2.getSynset().getPartOfSpeech())) {
					sensePairs.put(new SensePair(sense1, sense2), wordSenseDisambiguator
							.relatedness(sense1, sense2));
				}
			}
		}

		SenseRelationInfo maxRelatedness = null;
		Set<SensePair> actualMostRelated = new HashSet<SensePair>();
		for (SensePair key : sensePairs.keySet()) {
			SenseRelationInfo relatedness = sensePairs.get(key);
			if ((maxRelatedness == null) || (relatedness.getRank() > maxRelatedness.getRank())) {
				maxRelatedness = relatedness;
				actualMostRelated.clear();
				actualMostRelated.add(key);
			} else if (relatedness == maxRelatedness) {
				actualMostRelated.add(key);
			}
			if (relatedness.getRank() > 0.0) {
				log.info(key + " -> " + relatedness);
			}
		}
		assertContains(expectedMostRelated, actualMostRelated);
	}

	private void assertMostDefinitionSimilarity(String lemma1, PartOfSpeech pos1, int senseRank1,
			String lemma2, PartOfSpeech pos2, int senseRank2) throws Exception {
		Map<SensePair, SenseRelationInfo> sensePairs = new HashMap<SensePair, SenseRelationInfo>();
		SensePair expectedMostRelated = makeSensePair(lemma1, pos1, senseRank1, lemma2, pos2,
				senseRank2);
		SenseRelationInfo expectedMaxRelatedness = wordSenseDisambiguator.definitionSimilarity(
				expectedMostRelated.getSense1(), expectedMostRelated.getSense2());
		log.info(expectedMostRelated + " -> " + expectedMaxRelatedness);

		Word word1 = expectedMostRelated.getSense1().getWord();
		Word word2 = expectedMostRelated.getSense2().getWord();

		// for relatedness we want the best senses in the expected parts of
		// speech
		for (Sense sense1 : word1.getSenses(pos1)) {
			for (Sense sense2 : word2.getSenses(pos2)) {
				// only compare different parts of speech
				if (!sense1.getSynset().getPartOfSpeech().equals(
						sense2.getSynset().getPartOfSpeech())) {
					sensePairs.put(new SensePair(sense1, sense2), wordSenseDisambiguator
							.definitionSimilarity(sense1, sense2));
				}
			}
		}

		SenseRelationInfo maxRelatedness = null;
		Set<SensePair> actualMostRelated = new HashSet<SensePair>();
		for (SensePair key : sensePairs.keySet()) {
			SenseRelationInfo relatedness = sensePairs.get(key);
			if ((maxRelatedness == null) || (relatedness.getRank() > maxRelatedness.getRank())) {
				maxRelatedness = relatedness;
				actualMostRelated.clear();
				actualMostRelated.add(key);
			} else if (relatedness == maxRelatedness) {
				actualMostRelated.add(key);
			}
			if (relatedness.getRank() > 0.0) {
				log.info(key + " -> " + relatedness);
			}
		}
		assertContains(expectedMostRelated, actualMostRelated);
	}

	protected static class SensePair {
		private final Sense sense1;
		private final Sense sense2;

		protected SensePair(Sense sense1, Sense sense2) {
			this.sense1 = sense1;
			this.sense2 = sense2;
		}

		protected Sense getSense1() {
			return sense1;
		}

		protected Sense getSense2() {
			return sense2;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((sense1 == null) ? 0 : sense1.hashCode());
			result = prime * result + ((sense2 == null) ? 0 : sense2.hashCode());
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
			if (getClass() != obj.getClass()) {
				return false;
			}
			final SensePair other = (SensePair) obj;
			if (sense1 == null) {
				if (other.sense1 != null) {
					return false;
				}
			} else if (!sense1.equals(other.sense1)) {
				return false;
			}
			if (sense2 == null) {
				if (other.sense2 != null) {
					return false;
				}
			} else if (!sense2.equals(other.sense2)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "(" + getSense1() + " [" + getSense1().getSynset().getDefinition() + "], ["
					+ getSense2().getSynset().getDefinition() + "] " + getSense2() + ")";
		}
	}
}
