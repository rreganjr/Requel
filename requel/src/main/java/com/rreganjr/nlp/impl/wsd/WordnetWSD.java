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
package com.rreganjr.nlp.impl.wsd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.GrammaticalStructureLevel;
import com.rreganjr.nlp.NLPProcessor;
import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.PartOfSpeech;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.Linkdef;
import com.rreganjr.nlp.dictionary.SemcorSentenceWord;
import com.rreganjr.nlp.dictionary.Sense;
import com.rreganjr.nlp.dictionary.Synset;
import com.rreganjr.nlp.dictionary.SynsetDefinitionWord;
import com.rreganjr.nlp.dictionary.Word;
import com.rreganjr.nlp.impl.wsd.ColocationSenseRelationInfo.ColocationSource;
import com.rreganjr.requel.NoSuchEntityException;

/**
 * A dictionary based word sense disambiguator that uses the information content
 * and semantic similarity of WordNet words to disambiguate terms.
 * 
 * @author ron
 */
@Component("WordNetWSD")
public class WordnetWSD implements NLPProcessor<NLPText> {
	private static final Log log = LogFactory.getLog(WordnetWSD.class);

	private final DictionaryRepository dictionaryRepository;

	/**
	 * @param dictionaryRepository
	 * @param processorFactory
	 */
	@Autowired
	public WordnetWSD(DictionaryRepository dictionaryRepository) {
		this.dictionaryRepository = dictionaryRepository;
	}

	@Override
	public NLPText process(NLPText text) {
		if (text.in(GrammaticalStructureLevel.PARAGRAPH)) {
			for (NLPText sentence : text.getChildren()) {
				process(sentence);
			}
		} else {
			// identify all the dictionary words with a single sense for the
			// given part of speech (monosemous words)
			for (NLPText word : text.getLeaves()) {
				Word dicWord = word.getDictionaryWord();
				if (dicWord != null) {
					Set<Sense> senses = dicWord.getSenses(word.getPartOfSpeech());
					if (senses.size() == 1) {
						word.setDictionaryWordSense(senses.iterator().next());
					}
				}
			}
			// disambiguate the remaining words
			disambiguate(text);
		}
		return text;
	}

	/**
	 * disambiguate using Wordnet similarity by shared info content.
	 * 
	 * @param text
	 * @param word
	 */
	public void disambiguate(NLPText text) {
		Map<NLPText, Set<SenseRelationInfo>> senseRelations = new HashMap<NLPText, Set<SenseRelationInfo>>();

		Set<PairOfWords> pairOfWords = getPairsOfWords(text);
		log.debug(pairOfWords);
		for (PairOfWords tuple : pairOfWords) {
			Sense bestSense1 = null;
			Sense bestSense2 = null;
			if (!senseRelations.containsKey(tuple.getWord1())) {
				senseRelations.put(tuple.getWord1(), new HashSet<SenseRelationInfo>());
			}
			if (!senseRelations.containsKey(tuple.getWord2())) {
				senseRelations.put(tuple.getWord2(), new HashSet<SenseRelationInfo>());
			}

			// if both words already have senses skip to next pair
			if ((tuple.getWord1().getDictionaryWordSense() != null)
					&& (tuple.getWord2().getDictionaryWordSense() != null)) {
				log.debug("pair " + tuple + " is already disambiguated");
				continue;
			}

			// find co-locations of the word senses in synset defintions
			log.debug("searching for synset definition colocations of " + tuple.getWord1()
					+ " and " + tuple.getWord2());
			List<Map<NLPText, Sense>> colocations;
			colocations = findSynsetDefinitionColocations(tuple.getWord1(), tuple.getWord2());
			log.debug("colocations of " + tuple.getWord1() + " and " + tuple.getWord2() + ": "
					+ colocations);
			if (!colocations.isEmpty()) {
				for (Map<NLPText, Sense> wordSenseMap : colocations) {
					if (wordSenseMap.containsKey(tuple.getWord1())
							&& wordSenseMap.containsKey(tuple.getWord2())) {
						bestSense1 = wordSenseMap.get(tuple.getWord1());
						bestSense2 = wordSenseMap.get(tuple.getWord2());

						SenseRelationInfo sri = new ColocationSenseRelationInfo(
								ColocationSource.WordNetDefinition, bestSense1, bestSense2, "");
						senseRelations.get(tuple.getWord1()).add(sri);
						senseRelations.get(tuple.getWord2()).add(sri);
					}
				}
			}

			// find co-locations of the word senses in the semcor corpus
			// sentences
			log.debug("searching for Semcor Corpus sentence colocations of " + tuple.getWord1()
					+ " and " + tuple.getWord2());
			colocations = findSemcorSentenceColocations(tuple.getWord1(), tuple.getWord2());
			log.debug("colocations of " + tuple.getWord1() + " and " + tuple.getWord2() + ": "
					+ colocations);
			if (!colocations.isEmpty()) {
				for (Map<NLPText, Sense> wordSenseMap : colocations) {
					if (wordSenseMap.containsKey(tuple.getWord1())
							&& wordSenseMap.containsKey(tuple.getWord2())) {
						bestSense1 = wordSenseMap.get(tuple.getWord1());
						bestSense2 = wordSenseMap.get(tuple.getWord2());

						SenseRelationInfo sri = new ColocationSenseRelationInfo(
								ColocationSource.SemcorCorpus, bestSense1, bestSense2, "");
						senseRelations.get(tuple.getWord1()).add(sri);
						senseRelations.get(tuple.getWord2()).add(sri);
					}
				}
			}

			// similarity of word senses or their definitions
			for (Sense word1Sense : tuple.getWord1().getDictionaryWord().getSenses(
					tuple.getWord1().getPartOfSpeech())) {
				for (Sense word2Sense : tuple.getWord2().getDictionaryWord().getSenses(
						tuple.getWord2().getPartOfSpeech())) {
					SenseRelationInfo sri;
					if (word1Sense.getSynset().getPartOfSpeech().equals(
							word2Sense.getSynset().getPartOfSpeech())) {
						sri = similarity(word1Sense, word2Sense);
					} else {
						sri = definitionSimilarity(word1Sense, word2Sense);
					}
					senseRelations.get(tuple.getWord1()).add(sri);
					senseRelations.get(tuple.getWord2()).add(sri);

					// relatedness
					sri = relatedness(word1Sense, word2Sense);
					senseRelations.get(tuple.getWord1()).add(sri);
					senseRelations.get(tuple.getWord2()).add(sri);
				}
			}

			if (log.isDebugEnabled()) {
				for (NLPText word : text.getLeaves()) {
					log.debug(word.getText() + " -> " + senseRelations.get(word));
				}
			}

			// build a sense by sense matrix and assign a rank of the
			// relationship between each sense.
			int senseCount = 0;
			Map<Sense, Integer> senseToIndexMap = new HashMap<Sense, Integer>();
			Map<Integer, Sense> indexToSenseMap = new HashMap<Integer, Sense>();
			for (NLPText word : text.getLeaves()) {
				if (word.getDictionaryWord() != null) {
					for (Sense sense : word.getDictionaryWord().getSenses()) {
						senseToIndexMap.put(sense, senseCount);
						indexToSenseMap.put(senseCount, sense);
						senseCount++;
					}
				}
			}
			Counter senseRelationMatrix[][] = new Counter[senseCount][senseCount];
			for (NLPText word : text.getLeaves()) {
				Set<SenseRelationInfo> relations = senseRelations.get(word);
				if (relations != null) {
					for (SenseRelationInfo sri : relations) {
						Sense sense1 = sri.getSense1();
						Sense sense2 = sri.getSense2();
						int index1 = senseToIndexMap.get(sense1);
						int index2 = senseToIndexMap.get(sense2);
						Counter rank = senseRelationMatrix[index1][index2];
						if (rank == null) {
							rank = new Counter();
							senseRelationMatrix[index1][index2] = rank;
						}
						rank.count += sri.getRank();
					}
				}
			}

			// sum up the ranks of each sense
			Map<Sense, Counter> senseRankMap = new HashMap<Sense, Counter>();
			for (int x = 0; x < senseCount; x++) {
				for (int y = 0; y < senseCount; y++) {
					if (x != y) {
						Sense sense = indexToSenseMap.get(x);
						Counter rank = senseRelationMatrix[x][y];
						if (rank != null) {
							Counter counter = senseRankMap.get(sense);
							if (counter == null) {
								counter = new Counter();
								senseRankMap.put(sense, counter);
							}
							counter.count += rank.count;
						}
					}
				}
			}

			// assign the sense of a word with the highest rank
			for (NLPText word : text.getLeaves()) {
				if (word.getDictionaryWord() != null) {
					double maxRank = 0.0;
					// choose the highest ranked in WordNet for each word.
					Sense bestSense = word.getDictionaryWord().getSense(word.getPartOfSpeech(), 1);
					for (Sense sense : word.getDictionaryWord().getSenses()) {
						Counter rankCounter = senseRankMap.get(sense);
						if (rankCounter != null) {
							double rank = senseRankMap.get(sense).count;
							if (rank > maxRank) {
								maxRank = rank;
								bestSense = sense;
							}
						}
					}
					word.setDictionaryWordSense(bestSense);
					word.setDictionaryWordSenseRelationInfo(senseRelations.get(word));
				}
			}
		}
	}

	/**
	 * Example:<br>
	 * text -> users access the system.<br>
	 * results:<br>
	 * user, access<br>
	 * user, system<br>
	 * access, system<br>
	 * 
	 * @param text
	 * @return the distinct pairs of all the dictionary words in the text not
	 *         including pairs of the same word
	 */
	protected Set<PairOfWords> getPairsOfWords(NLPText text) {
		Set<PairOfWords> pairsOfWords = new HashSet<PairOfWords>();
		for (NLPText word1 : text.getLeaves()) {
			if (word1.getDictionaryWord() != null) {
				for (NLPText word2 : text.getLeaves()) {
					if ((word2.getDictionaryWord() != null)
							&& (!word1.getLemma().equals(word2.getLemma()) || !word1
									.getPartOfSpeech().equals(word2.getPartOfSpeech()))) {
						pairsOfWords.add(new PairOfWords(word1, word2));
					}
				}
			}
		}
		return pairsOfWords;
	}

	/**
	 * Given two words find the senses of the words that colocate in the synset
	 * defintions of WordNet.
	 * 
	 * @param word1
	 * @param word2
	 * @return a list of the senses of the supplied words that colocate mapped
	 *         by the word. If the words don't colocate an empty list is
	 *         returned. If one of the words is already assigned a sense return
	 *         only the senses of the other word that colocate with that sense.
	 */
	public List<Map<NLPText, Sense>> findSynsetDefinitionColocations(NLPText word1, NLPText word2) {
		List<Map<NLPText, Sense>> colocations = new ArrayList<Map<NLPText, Sense>>();
		List<Synset> synsets;
		if (word1.getDictionaryWordSense() != null) {
			synsets = dictionaryRepository.findSynsetsWithColocatedDefinitionSenseAndWord(word1
					.getDictionaryWordSense(), word2.getDictionaryWord());
		} else if (word2.getDictionaryWordSense() != null) {
			synsets = dictionaryRepository.findSynsetsWithColocatedDefinitionSenseAndWord(word2
					.getDictionaryWordSense(), word1.getDictionaryWord());
		} else {
			synsets = dictionaryRepository.findSynsetsWithColocatedDefinitionWords(word1
					.getDictionaryWord(), word2.getDictionaryWord());
		}
		for (Synset synset : synsets) {
			Set<Sense> word1Senses = new HashSet<Sense>();
			Set<Sense> word2Senses = new HashSet<Sense>();

			if (synset.getWords() != null) {
				for (SynsetDefinitionWord defWord : synset.getWords()) {
					if ((defWord != null) && defWord.getText().equals(word1.getLemma())
							&& synset.isPartOfSpeech(word1.getPartOfSpeech())) {
						log.debug(word1.getLemma() + "#" + defWord.getSense().getRank() + " "
								+ defWord.getSynset().getDefinition());
						word1Senses.add(defWord.getSense());
					}
					if ((defWord != null) && defWord.getText().equals(word2.getLemma())
							&& synset.isPartOfSpeech(word2.getPartOfSpeech())) {
						log.debug(word2.getLemma() + "#" + defWord.getSense().getRank() + " "
								+ defWord.getSynset().getDefinition());
						word2Senses.add(defWord.getSense());
					}
				}
			}
			// make pairs for each of the word1 and word2 senses
			for (Sense sense1 : word1Senses) {
				for (Sense sense2 : word2Senses) {
					Map<NLPText, Sense> senseMap = new HashMap<NLPText, Sense>();
					senseMap.put(word1, sense1);
					senseMap.put(word2, sense2);
					colocations.add(senseMap);
				}
			}
		}

		return colocations;
	}

	/**
	 * Given two words find the senses of the words that colocate in the Semcor
	 * Corpus sentences.
	 * 
	 * @param word1
	 * @param word2
	 * @return a list of the senses of the supplied words that colocate mapped
	 *         by the word. If the words don't colocate an empty list is
	 *         returned. If one of the words is already assigned a sense return
	 *         only the senses of the other word that colocate with that sense.
	 */
	public List<Map<NLPText, Sense>> findSemcorSentenceColocations(NLPText word1, NLPText word2) {
		List<Map<NLPText, Sense>> colocations = new ArrayList<Map<NLPText, Sense>>();
		List<SemcorSentenceWord> sentenceWords;
		if (word1.getDictionaryWordSense() != null) {
			sentenceWords = dictionaryRepository
					.findSemcorSentencesWithColocatedDefinitionSenseAndWord(word1
							.getDictionaryWordSense(), word2.getDictionaryWord());
		} else if (word2.getDictionaryWordSense() != null) {
			sentenceWords = dictionaryRepository
					.findSemcorSentencesWithColocatedDefinitionSenseAndWord(word2
							.getDictionaryWordSense(), word1.getDictionaryWord());
		} else {
			sentenceWords = dictionaryRepository.findSemcorSentencesWithColocatedDefinitionWords(
					word1.getDictionaryWord(), word2.getDictionaryWord());
		}
		Set<Sense> word1Senses = new HashSet<Sense>();
		Set<Sense> word2Senses = new HashSet<Sense>();
		for (SemcorSentenceWord sentenceWord : sentenceWords) {
			log.debug(sentenceWord);
			if (sentenceWord.getSense().getWord().getLemma().equals(word1.getLemma())) {
				log.debug(word1.getLemma() + "#" + sentenceWord.getSense().getRank() + " "
						+ sentenceWord.getSense().getSynset().getDefinition());
				word1Senses.add(sentenceWord.getSense());
			}
			if (sentenceWord.getSense().getWord().getLemma().equals(word2.getLemma())) {
				log.debug(word2.getLemma() + "#" + sentenceWord.getSense().getRank() + " "
						+ sentenceWord.getSense().getSynset().getDefinition());
				word2Senses.add(sentenceWord.getSense());
			}
		}

		// make pairs for each of the word1 and word2 senses
		for (Sense sense1 : word1Senses) {
			for (Sense sense2 : word2Senses) {
				Map<NLPText, Sense> senseMap = new HashMap<NLPText, Sense>();
				senseMap.put(word1, sense1);
				senseMap.put(word2, sense2);
				colocations.add(senseMap);
			}
		}
		return colocations;
	}

	/**
	 * The relationship of two senses through derivation, pertainym or
	 * entailment.
	 * 
	 * @param sense1
	 * @param sense2
	 * @return
	 */
	public SenseRelationInfo relatedness(Sense sense1, Sense sense2) {
		log.debug("sense1: " + sense1 + " " + sense1.getSynset().getDefinition());
		log.debug("sense2: " + sense2 + " " + sense2.getSynset().getDefinition());

		if (sense1.equals(sense2)) {
			return new SemanticSimilaritySenseRelationInfo(sense1, sense2, 1.0d, "identical.");
		}
		Linkdef linkDef;
		try {
			linkDef = dictionaryRepository.findLinkDef(40L);
			dictionaryRepository.findSemlinkref(sense1.getSynset(), sense2.getSynset(), linkDef, 1);
			return new SemanticRelatednessSenseRelationInfo(sense1, sense2, 1.0d, linkDef.getName());
		} catch (NoSuchEntityException e) {

		}

		for (Long linkDefId : new Long[] { 71L, 80L, 81L }) {
			try {
				linkDef = dictionaryRepository.findLinkDef(linkDefId);
				dictionaryRepository.findLexlinkref(sense1, sense2, linkDef);
				return new SemanticRelatednessSenseRelationInfo(sense1, sense2, 1.0d, linkDef
						.getName());
			} catch (NoSuchEntityException e) {

			}
		}
		return new SemanticRelatednessSenseRelationInfo(sense1, sense2, 0.0d, "unrelated.");
	}

	/**
	 * The similarity of the words in the definitions of the two word senses and
	 * the two senses themselves.
	 * 
	 * @param sense1
	 * @param sense2
	 * @return
	 */
	public SenseRelationInfo definitionSimilarity(Sense sense1, Sense sense2) {
		log.debug("sense1: " + sense1 + " " + sense1.getSynset().getDefinition());
		log.debug("sense2: " + sense2 + " " + sense2.getSynset().getDefinition());

		if (sense1.equals(sense2)) {
			return new SemanticSimilaritySenseRelationInfo(sense1, sense2, 1.0d, "identical.");
		}

		int sense1Dim = sense1.getSynset().getWords().size() + 1;
		int sense2Dim = sense2.getSynset().getWords().size() + 1;
		SenseRelationInfo senseRelations[][] = new SenseRelationInfo[sense1Dim][sense2Dim];
		for (SynsetDefinitionWord sense1defWord : sense1.getSynset().getWords()) {
			Sense sense1defWordSense = sense1defWord.getSense();
			log.debug("sense1defWord = " + sense1defWord);
			if ((sense1defWordSense != null)
					&& sense1defWordSense.getSynset().getPartOfSpeech().in(PartOfSpeech.NOUN,
							PartOfSpeech.VERB)) {
				// compare the sense 1 definition word sense to sense 2
				if (sense1defWordSense.getSynset().getPartOfSpeech().equals(
						sense2.getSynset().getPartOfSpeech())) {
					SenseRelationInfo similarity = similarity(sense1defWordSense, sense2);
					log.debug("  sense1 def word: " + sense1defWordSense + " sense2: " + sense2
							+ " similiarity: " + similarity.getRank());
					senseRelations[sense1.getSynset().getWords().indexOf(sense1defWordSense) + 1][0] = similarity;
				}
				for (SynsetDefinitionWord sense2defWord : sense2.getSynset().getWords()) {
					Sense sense2defWordSense = sense2defWord.getSense();
					log.debug("  sense2defWord = " + sense2defWord);
					if (sense2defWordSense != null) {
						// compare the sense 2 definition word senses to
						// sense 1
						if (sense2defWordSense.getSynset().getPartOfSpeech().equals(
								sense1.getSynset().getPartOfSpeech())) {
							SenseRelationInfo similarity = similarity(sense1, sense2defWordSense);
							log.debug("  sense1: " + sense1 + " sense2 def word: "
									+ sense2defWordSense + " similiarity: " + similarity.getRank());
							senseRelations[0][sense2.getSynset().getWords().indexOf(
									sense1defWordSense) + 1] = similarity;
						}
						// compare the sense 2 definition word sense to the
						// sense 1 definition word sense
						if (sense1defWordSense.getSynset().getPartOfSpeech().equals(
								sense2defWordSense.getSynset().getPartOfSpeech())) {
							SenseRelationInfo similarity = similarity(sense1defWordSense,
									sense2defWordSense);
							log.debug("  sense1 def word: " + sense1defWordSense
									+ " sense2 def word: " + sense2defWordSense + " similiarity: "
									+ similarity.getRank());
							senseRelations[sense1.getSynset().getWords()
									.indexOf(sense1defWordSense) + 1][sense2.getSynset().getWords()
									.indexOf(sense1defWordSense) + 1] = similarity;
						}
					}
				}
			}
		}
		double similaritySum = 0.0d;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < sense1Dim; i++) {
			for (int j = 0; j < sense2Dim; j++) {
				if (senseRelations[i][j] != null) {
					similaritySum += senseRelations[i][j].getRank();
					sb.append(senseRelations[i][j].getReason());
					sb.append("\n");
				}
			}
		}
		return new SemanticRelatednessSenseRelationInfo(sense1, sense2, similaritySum
				/ (sense1Dim + sense2Dim), sb.toString());
	}

	/**
	 * Given two synsets of like part of speech, calculate the similarity of the
	 * words using the information content measure of Seco, Veale and Hayes and
	 * the similarity measure defined by Lin, which is a factor of the
	 * commonality of the two terms over their combined specificity.<br>
	 * NOTE: in the cases where two synsets have multiple common ancestors, the
	 * highest similarity is returned.
	 * 
	 * @param sense1
	 * @param sense2
	 * @param linkType
	 * @return
	 */
	public SenseRelationInfo similarity(Sense sense1, Sense sense2) {
		log.debug("sense1: " + sense1 + " " + sense1.getSynset().getDefinition());
		log.debug("sense2: " + sense2 + " " + sense2.getSynset().getDefinition());
		Linkdef linkType = dictionaryRepository.findLinkDef(1L);
		Synset synset1 = sense1.getSynset();
		Synset synset2 = sense2.getSynset();

		// TODO: link types available, for example Pieces and Cancer are
		// related as zodiac signs, but use linkType of 4 (instance of) and not
		// hyponym.
		if (synset1.equals(synset2)) {
			return new SemanticSimilaritySenseRelationInfo(sense1, sense2, 1.0d, "identical.");
		}

		SenseRelationInfo sri = new SemanticSimilaritySenseRelationInfo(sense1, sense2, 0.0d,
				"no similarity.");
		for (Synset lcs : dictionaryRepository.getLowestCommonHypernyms(synset1, synset2)) {
			// Similarity using Lin's formula
			double ic1 = dictionaryRepository.infoContent(synset1, linkType);
			double ic2 = dictionaryRepository.infoContent(synset2, linkType);
			double icLCS = dictionaryRepository.infoContent(lcs, linkType);
			double similarity = (2.0d * icLCS) / (ic1 + ic2);
			// Jiang and Conrath 1997 - similarity
			// (2.0d * icLCS) - (ic1 + ic2)
			if (similarity > sri.getRank()) {
				sri = new SemanticSimilaritySenseRelationInfo(sense1, sense2, similarity, sense1
						+ " ic: " + ic1 + " " + sense2 + " ic: " + ic2 + " lcs: " + lcs + " ic: "
						+ icLCS);
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("synset1: " + synset1 + " synset2: " + synset2 + " maxSimilarity: "
					+ sri.getRank());
		}
		return sri;
	}

	protected class PairOfWords {
		private final NLPText word1;
		private final NLPText word2;

		protected PairOfWords(NLPText word1, NLPText word2) {
			// put the words in order of spelling
			if (word1.getLemma().compareTo(word2.getLemma()) < 0) {
				this.word1 = word1;
				this.word2 = word2;
			} else {
				this.word1 = word2;
				this.word2 = word1;
			}
		}

		protected NLPText getWord1() {
			return word1;
		}

		protected NLPText getWord2() {
			return word2;
		}

		@Override
		public int hashCode() {
			return word1.hashCode() + word2.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof PairOfWords) {
				PairOfWords other = (PairOfWords) o;
				if (((word1.getText().equals(other.word1.getText()) && word1.getPartOfSpeech()
						.equals(other.word1.getPartOfSpeech())))
						&& ((word2.getText().equals(other.word2.getText()) && word2
								.getPartOfSpeech().equals(other.word2.getPartOfSpeech())))) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return "(" + getWord1() + ", " + getWord2() + ")";
		}
	}

	private class Counter {
		double count = 0.0;
	}
}
