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
package edu.harvard.fas.rregan.nlp.impl.lemmatizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.impl.repository.NoSuchWordException;

/**
 * Convert a word to its lemma by systematically applying rules that replace a
 * suffix with a new suffix and checking if the manufactured string is in the
 * dictionary.<br>
 * For Example:<br>
 * if a noun ends in 's' replace the 's' with '' and see if the word exists.<br>
 * if a noun ends in 'ses' replace the 'ses' with 's' and see if the word
 * exists.<br>
 * <br>
 * The rules are applied by trying to replace the longest suffixes first.
 * 
 * @author ron
 */
public class DictionarySuffixExchangingLemmatizerRule extends AbstractDictionaryLemmatizerRule {

	private final List<String> suffixExchangesFrom;
	private final List<String> suffixExchangesTo;
	private final Set<PartOfSpeech> appliesToPartsOfSpeech;

	/**
	 * @param dictionaryRepository -
	 *            the dictionary repository to look up a possible lemma after
	 *            applying the rule to see if it matches a known word.
	 * @param appliesToPartsOfSpeech -
	 *            a list of the parts of speech this rule applies to.
	 * @param suffixExchangesFrom -
	 *            a list of the suffix to remove from the word and replaced by
	 *            the corresponding string in suffixExchangesTo
	 * @param suffixExchangesTo -
	 *            a list of the suffix to append to the word to create the lemma
	 *            after removing the suffix corresponding to this string in
	 *            suffixExchangesFrom
	 */
	public DictionarySuffixExchangingLemmatizerRule(DictionaryRepository dictionaryRepository,
			Set<PartOfSpeech> appliesToPartsOfSpeech, List<String> suffixExchangesFrom,
			List<String> suffixExchangesTo) {
		super(dictionaryRepository);
		this.appliesToPartsOfSpeech = appliesToPartsOfSpeech;
		this.suffixExchangesFrom = suffixExchangesFrom;
		this.suffixExchangesTo = suffixExchangesTo;
		sortSuffixesByLength();
	}

	/**
	 * sort the suffixes by length, the longest first. suffixes of the same
	 * length are in arbitrary order.
	 */
	private void sortSuffixesByLength() {
		int maxSuffixLength = 0;
		Map<Integer, Set<Integer>> suffixesByLength = new HashMap<Integer, Set<Integer>>();
		for (int index = 0; index < suffixExchangesFrom.size(); index++) {
			String suffix = suffixExchangesFrom.get(index);
			if (!suffixesByLength.containsKey(suffix.length())) {
				if (maxSuffixLength < suffix.length()) {
					maxSuffixLength = suffix.length();
				}
				suffixesByLength.put(suffix.length(), new HashSet<Integer>());
			}
			suffixesByLength.get(suffix.length()).add(index);
		}
		int swapIndex = 0;
		for (int length = maxSuffixLength; length > 0; length--) {
			Set<Integer> suffixesOfLength = suffixesByLength.get(length);
			if (suffixesOfLength != null) {
				for (int index : suffixesOfLength) {
					String tmpFrom = suffixExchangesFrom.get(swapIndex);
					String tmpTo = suffixExchangesTo.get(swapIndex);
					suffixExchangesFrom.set(swapIndex, suffixExchangesFrom.get(index));
					suffixExchangesTo.set(swapIndex, suffixExchangesTo.get(index));
					suffixExchangesFrom.set(index, tmpFrom);
					suffixExchangesTo.set(index, tmpTo);
					swapIndex++;
				}
			}
		}
	}

	@Override
	public String lemmatize(String word, PartOfSpeech partOfSpeech) {
		if (appliesToPartsOfSpeech.contains(partOfSpeech)) {
			for (int index = 0; index < suffixExchangesFrom.size(); index++) {
				String suffix = suffixExchangesFrom.get(index);
				if (word.endsWith(suffix)) {
					String lemma = word.substring(0, word.length() - suffix.length())
							+ suffixExchangesTo.get(index);
					try {
						return getDictionaryRepository().findWord(lemma, partOfSpeech).getLemma();
					} catch (NoSuchWordException e) {
						// keep trying
					}
				}
			}
		}
		return null;
	}
}
