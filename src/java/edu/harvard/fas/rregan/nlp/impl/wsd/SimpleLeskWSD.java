/*
 * $Id: SimpleLeskWSD.java,v 1.4 2009/01/26 10:19:05 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.wsd;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPProcessorFactory;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.SemcorSentence;
import edu.harvard.fas.rregan.nlp.dictionary.SemcorSentenceWord;
import edu.harvard.fas.rregan.nlp.dictionary.Sense;
import edu.harvard.fas.rregan.nlp.dictionary.Word;
import edu.harvard.fas.rregan.nlp.impl.NLPTextImpl;

/**
 * A dictionary based word sense disambiguator that uses the simplified lesk
 * algorithm.
 * 
 * @author ron
 */
// @Component("SimpleLeskWSD")
public class SimpleLeskWSD implements NLPProcessor<NLPText> {
	private static final Logger log = Logger.getLogger(SimpleLeskWSD.class);

	private final DictionaryRepository dictionaryRepository;
	private final NLPProcessor<NLPText> sentencizer;
	private final NLPProcessor<NLPText> parser;
	private final NLPProcessor<NLPText> lemmatizer;
	private final NLPProcessor<NLPText> dictionizer;

	/**
	 * @param dictionaryRepository
	 * @param processorFactory
	 */
	@Autowired
	public SimpleLeskWSD(DictionaryRepository dictionaryRepository,
			NLPProcessorFactory processorFactory) {
		this.dictionaryRepository = dictionaryRepository;
		sentencizer = processorFactory.getSentencizer();
		parser = processorFactory.getParser();
		lemmatizer = processorFactory.getLemmatizer();
		dictionizer = processorFactory.getDictionizer();
	}

	@Override
	public NLPText process(NLPText text) {
		if (text.in(GrammaticalStructureLevel.PARAGRAPH)) {
			for (NLPText sentence : text.getChildren()) {
				process(sentence);
			}
		} else {
			dictionizer.process(text);
			for (NLPText word : text.getLeaves()) {
				Word dicWord = word.getDictionaryWord();
				if (dicWord != null) {
					if (dicWord.getSenses().size() == 1) {
						word.setDictionaryWordSense(dicWord.getSenses().iterator().next());
					} else if (dicWord.getSenses().size() > 1) {
						disambiguate(text, word);
					}
				}
			}
		}
		return text;
	}

	/**
	 * Simplified Lesk algorithm from Speech and Language Processing, second
	 * edition by Daniel Jurafsky and James Martin, page 646.
	 * 
	 * @param text
	 * @param word
	 */
	private void disambiguate(NLPText text, NLPText word) {
		Sense bestSense = word.getDictionaryWord().getSense(word.getPartOfSpeech(), 1);
		int maxOverlap = 0;
		for (Sense sense : word.getDictionaryWord().getSenses(word.getPartOfSpeech())) {
			// TODO: preprocess all the senses
			NLPText signature = buildSignature(sense);
			int overlap = computeOverlap(signature, text);
			if (overlap > maxOverlap) {
				maxOverlap = overlap;
				bestSense = sense;
			} else if (overlap == maxOverlap) {
				// for equal overlap use the more common form
				if (sense.getRank() < bestSense.getRank()) {
					bestSense = sense;
				}
			}
		}
		word.setDictionaryWordSense(bestSense);
	}

	private NLPText buildSignature(Sense sense) {
		NLPTextImpl definition = new NLPTextImpl(sense.getSynset().getDefinition());
		sentencizer.process(definition);
		parser.process(definition);
		lemmatizer.process(definition);
		dictionizer.process(definition);
		NLPTextImpl signature = new NLPTextImpl(null, GrammaticalStructureLevel.BAGOFWORDS);
		for (NLPText word : definition.getLeaves()) {
			signature.getChildren().add(word);
		}
		// add all semcor sentence words to build up the signature
		for (SemcorSentence sentence : dictionaryRepository.findSemcorSentences(sense)) {
			for (SemcorSentenceWord word : sentence.getWords()) {
				if (word.getSense() != null) {
					NLPTextImpl nlpWord = new NLPTextImpl(signature, word.getText(),
							GrammaticalStructureLevel.WORD);
					signature.getChildren().add(nlpWord);
					nlpWord.setParseTag(word.getParseTag());
					nlpWord.setPartOfSpeech(word.getParseTag().getPartOfSpeech());
					Sense aSense = word.getSense();
					nlpWord.setDictionaryWordSense(aSense);
					nlpWord.setPartOfSpeech(aSense.getSynset().getPartOfSpeech());
					nlpWord.setDictionaryWord(aSense.getWord());
					nlpWord.setLemma(aSense.getWord().getLemma());
				}
			}
		}
		return signature;
	}

	private int computeOverlap(NLPText signature, NLPText context) {
		int overlap = 0;
		log.debug("signature: " + signature);
		log.debug("context: " + context);
		for (NLPText sigWord : signature.getLeaves()) {
			for (NLPText contextWord : context.getLeaves()) {
				if ((sigWord.getDictionaryWord() != null)
						&& sigWord.getDictionaryWord().equals(contextWord.getDictionaryWord())) {
					log.info("dictionary match: " + sigWord.getDictionaryWord() + " "
							+ contextWord.getDictionaryWord());
					overlap++;
				}
			}
		}
		log.debug("overlap: " + overlap);
		return overlap;
	}
}
