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
package edu.harvard.fas.rregan.nlp.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.ParserException;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.Sense;
import edu.harvard.fas.rregan.nlp.dictionary.Word;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PositionAnnotation;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreebankLanguagePack;

/**
 * Wrapper for Stanford Named Entity Recognzier.
 * 
 * @author ron
 */
@Component("stanfordNameEntityRecognizer")
public class StanfordNameEntityRecognizer implements NLPProcessor<NLPText> {
	private static final Logger log = Logger.getLogger(StanfordNameEntityRecognizer.class);

	/**
	 * The name of the property in the StanfordNameEntityRecognizer.properties
	 * file that contains the path to the serialized/zipped model relative to
	 * the classpath. By default the path is
	 * "resources/nlp/stanford-ner/ner-eng-ie.crf-3-all2008-distsim.ser.gz"
	 */
	public static final String PROP_MODEL_FILE = "StanfordNameEntityRecognizerModelFile";

	/**
	 * The default parser model
	 */
	public static final String PROP_MODEL_FILE_DEFAULT = "resources/nlp/stanford-ner/ner-eng-ie.crf-3-all2008-distsim.ser.gz";

	private static AbstractSequenceClassifier classifier;
	private static final TreebankLanguagePack tlp = new PennTreebankLanguagePack();

	static {
		try {
			ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
					StanfordLexicalizedParser.class.getName());

			String parserFile = resourceBundleHelper.getString(PROP_MODEL_FILE,
					PROP_MODEL_FILE_DEFAULT);

			classifier = CRFClassifier.getClassifier(new GZIPInputStream(
					StanfordNameEntityRecognizer.class.getClassLoader().getResourceAsStream(
							parserFile)));

		} catch (Exception e) {
			classifier = null;
			throw new ExceptionInInitializerError(e);
		}
	}

	private final DictionaryRepository dictionaryRepository;
	private Sense personSense;
	private Sense locationSense;
	private Sense organizationSense;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public StanfordNameEntityRecognizer(DictionaryRepository dictionaryRepository) {
		this.dictionaryRepository = dictionaryRepository;
	}

	@Override
	public NLPText process(NLPText text) {
		if (!initialized()) {
			init();
		}

		if (text.hasText()) {
			if (GrammaticalStructureLevel.PARAGRAPH.equals(text.getGrammaticalStructureLevel())) {
				for (NLPText sentence : text.getChildren()) {
					process(sentence);
				}
			} else if (GrammaticalStructureLevel.SENTENCE.equals(text
					.getGrammaticalStructureLevel())) {
				ner(text);
			}
		}
		return text;
	}

	/**
	 * The entity recognizer is created before the database is initialized so
	 * initialization must be done after it is created.
	 */
	private void init() {
		Word personWord = dictionaryRepository.findWord("person", PartOfSpeech.NOUN);
		personSense = personWord.getSense(PartOfSpeech.NOUN, 1);

		Word locationWord = dictionaryRepository.findWord("location", PartOfSpeech.NOUN);
		locationSense = locationWord.getSense(PartOfSpeech.NOUN, 1);

		Word organizationWord = dictionaryRepository.findWord("organization", PartOfSpeech.NOUN);
		organizationSense = organizationWord.getSense(PartOfSpeech.NOUN, 1);
	}

	private boolean initialized() {
		return !((personSense == null) || (locationSense == null) || (organizationSense == null));
	}

	private synchronized void ner(NLPText text) throws ParserException {
		List<TaggedWord> xtokens = new ArrayList<TaggedWord>();
		for (NLPText word : text.getLeaves()) {
			if (word.getParseTag() != null) {
				xtokens.add(new TaggedWord(word.getText(), word.getParseTag().getText()));
			} else {
				xtokens.add(new TaggedWord(word.getText()));
			}
		}
		List<? extends HasWord> tokens = xtokens;

		for (CoreLabel label : classifier.testSentence(tokens)) {
			String entityType = label.get(AnswerAnnotation.class);
			String positionText = label.get(PositionAnnotation.class);
			int position = Integer.parseInt(positionText);
			NLPText word = text.getLeaves().get(position);
			if ("PERSON".equals(entityType)) {
				word.setDictionaryWord(personSense.getWord());
				word.setDictionaryWordSense(personSense);
				word.setNamedEntity(true);
			} else if ("ORGANIZATION".equals(entityType)) {
				word.setDictionaryWord(organizationSense.getWord());
				word.setDictionaryWordSense(organizationSense);
				word.setNamedEntity(true);
			} else if ("LOCATION".equals(entityType)) {
				word.setDictionaryWord(locationSense.getWord());
				word.setDictionaryWordSense(locationSense);
				word.setNamedEntity(true);
			}
		}
	}

	private static String prepareSentence(String sentence) {
		sentence = sentence.replaceAll(" *, *", " , ");
		sentence = sentence.replaceAll(" *\\. *", " \\. ");
		sentence = sentence.replaceAll(" *\\! *", " \\! ");
		sentence = sentence.replaceAll(" *\\? *", " \\? ");
		sentence = sentence.replaceAll(" *: *", " : ");
		sentence = sentence.replaceAll(" *; *", " ; ");
		sentence = sentence.replaceAll(" *\\( *", " \\( ");
		sentence = sentence.replaceAll(" *\\) *", " \\) ");
		sentence = sentence.replaceAll(" *\\[ *", " \\[ ");
		sentence = sentence.replaceAll(" *\\] *", " \\] ");
		sentence = sentence.replaceAll(" *\\{ *", " \\{ ");
		sentence = sentence.replaceAll(" *\\} *", " \\} ");
		sentence = sentence.replaceAll(" *< *", " < ");
		sentence = sentence.replaceAll(" *> *", " > ");
		sentence = removeBrackets(sentence);
		return sentence;
	}

	private static String removeBrackets(String sentence) {
		sentence = removeBrackets(sentence, "(", ")");
		sentence = removeBrackets(sentence, "[", "]");
		sentence = removeBrackets(sentence, "{", "}");
		sentence = removeBrackets(sentence, "<", ">");
		return sentence;
	}

	private static String removeBrackets(String sentence, String openBracket, String closeBracket) {
		int openIdx = 0;
		while (openIdx != -1) {
			openIdx = sentence.indexOf(openBracket);
			if (openIdx == -1) {
				return sentence;
			}
			int closeIdx = sentence.indexOf(closeBracket, openIdx);
			if (closeIdx == -1) {
				closeIdx = sentence.length() - 1;
			}
			String start = sentence.substring(0, openIdx);
			String end = sentence.substring(closeIdx + 1, sentence.length());
			sentence = start + end;
		}
		return sentence;
	}
}
