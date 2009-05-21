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

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.ApplicationException;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPText;

/**
 * Detects and separates sentences in an NLPText.
 * 
 * @author ron
 */
@Component("sentencizer")
public class Sentencizer extends AbstractOpenNLPTool<NLPText> {
	private static final Logger log = Logger.getLogger(Sentencizer.class);

	/**
	 * The name of the property in the NLPImpl.properties file that contains the
	 * path to the open nlp sentence detector model file relative to the
	 * classpath. By default the path is
	 * "resources/nlp/opennlp-tools/EnglishSD.bin.gz"
	 */
	public static final String PROP_ENGLISH_SENTENCE_DETECTOR_MODEL_FILE = "EnglishSentenceDetectorModelFile";

	/**
	 * The default sentence detector model file.
	 */
	public static final String PROP_ENGLISH_SENTENCE_DETECTOR_MODEL_FILE_DEFAULT = "resources/nlp/opennlp-tools/EnglishSD.bin.gz";

	private static SentenceDetector sentenceDetector;

	/**
	 * create a new Sentencizer, initializing if needed.
	 * 
	 * @throws ApplicationException
	 *             if the underlying OpenNLP sentence detector fails to
	 *             initalize
	 */
	public Sentencizer() {
		init();
	}

	private synchronized void init() {
		if (sentenceDetector == null) {
			try {
				ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
						Sentencizer.class.getName());

				String modelFile = resourceBundleHelper.getString(
						PROP_ENGLISH_SENTENCE_DETECTOR_MODEL_FILE,
						PROP_ENGLISH_SENTENCE_DETECTOR_MODEL_FILE_DEFAULT);

				sentenceDetector = new SentenceDetectorME(readGISModel(modelFile));
			} catch (Exception e) {
				sentenceDetector = null;
				throw ApplicationException.failedToInitializeComponent(getClass(), e);
			}
		}
	}

	@Override
	public NLPText process(NLPText text) {
		NLPTextImpl workingText = (NLPTextImpl) text;
		if (workingText.hasText()
				&& GrammaticalStructureLevel.UNKNOWN.equals(workingText
						.getGrammaticalStructureLevel())) {
			List<String> sentences = sentencize(workingText.getText());
			if (sentences.size() > 1) {
				workingText.setGrammaticalStructureLevel(GrammaticalStructureLevel.PARAGRAPH);

				int startIndex = 0;
				for (String sentence : sentences) {
					workingText.getChildren().add(
							new NLPTextImpl(workingText, sentence,
									GrammaticalStructureLevel.SENTENCE));
					startIndex += sentence.length();
				}
			} else {
				workingText.setGrammaticalStructureLevel(GrammaticalStructureLevel.SENTENCE);
			}
		}
		return text;
	}

	private List<String> sentencize(String text) {
		log.debug("text = " + text);
		if (sentenceDetector != null) {
			int[] sentenceOffsets = sentenceDetector.sentPosDetect(text);
			List<String> trimmedSentences = new ArrayList<String>(sentenceOffsets.length);

			if (sentenceOffsets.length == 0) {
				trimmedSentences.add(text);
			} else {
				// if leftover is true then there is dangling text after the
				// last sentence
				boolean leftover = sentenceOffsets[sentenceOffsets.length - 1] != text.length();
				trimmedSentences.add(text.substring(0, sentenceOffsets[0]).trim());

				for (int si = 1; si < sentenceOffsets.length; si++) {
					int nextStart = sentenceOffsets[si];
					while (Character.isWhitespace(text.charAt(nextStart - 1))) {
						nextStart--;
					}
					trimmedSentences.add(text.substring(sentenceOffsets[si - 1], nextStart));
				}
				if (leftover) {
					trimmedSentences.add(text
							.substring(sentenceOffsets[sentenceOffsets.length - 1]).trim());
				}
			}
			log.debug("sentences detected = " + trimmedSentences.size());
			return trimmedSentences;
		}
		return null;
	}
}
