/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.rreganjr.nlp.dictionary.impl.NLPTextImpl;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;

import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;
import opennlp.tools.util.InvalidFormatException;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.rreganjr.platform.ApplicationException;
import com.rreganjr.ResourceBundleHelper;
import com.rreganjr.nlp.dictionary.GrammaticalStructureLevel;
import com.rreganjr.nlp.dictionary.NLPText;

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
	 * "nlp/opennlp-tools/EnglishSD.bin.gz"
	 */
	public static final String PROP_ENGLISH_SENTENCE_DETECTOR_MODEL_FILE = "EnglishSentenceDetectorModelFile";

	/**
	 * The default sentence detector model file.
	 */
	public static final String PROP_ENGLISH_SENTENCE_DETECTOR_MODEL_FILE_DEFAULT = "nlp/opennlp-tools/EnglishSD.bin.gz";

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

				try (InputStream in = Sentencizer.class.getClassLoader().getResourceAsStream(modelFile)) {
					if (in == null) {
						throw new IOException("Sentence model not found on classpath: " + modelFile);
					}
					try {
						SentenceModel model = new SentenceModel(in);
						sentenceDetector = new SentenceDetectorME(model);
					} catch (Exception ife) {
						// Older bundled model may lack manifest.properties or be in legacy format; fall back to simple splitter
						log.warn("Falling back to simple sentence splitter because model could not be read: " + modelFile, ife);
						sentenceDetector = new SimpleSentenceDetector();
					}
				}
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
            Span[] sentenceOffsets = sentenceDetector.sentPosDetect(text);
			List<String> trimmedSentences = new ArrayList<String>(sentenceOffsets.length);

			if (sentenceOffsets.length == 0) {
				trimmedSentences.add(text);
			} else {
				// if leftover is true then there is dangling text after the
				// last sentence
				boolean leftover = sentenceOffsets[sentenceOffsets.length - 1].length() != text.length();
				trimmedSentences.add(text.substring(0, sentenceOffsets[0].length()).trim());

				for (int si = 1; si < sentenceOffsets.length; si++) {
					int nextStart = sentenceOffsets[si].length();
					while (Character.isWhitespace(text.charAt(nextStart - 1))) {
						nextStart--;
					}
					trimmedSentences.add(text.substring(sentenceOffsets[si - 1].length(), nextStart));
				}
				if (leftover) {
					trimmedSentences.add(text
							.substring(sentenceOffsets[sentenceOffsets.length - 1].length()).trim());
				}
			}
			log.debug("sentences detected = " + trimmedSentences.size());
			return trimmedSentences;
		}
		return null;
	}

	/**
	 * Very small fallback splitter used when the OpenNLP model cannot be loaded (e.g., legacy binary format).
	 */
	private static final class SimpleSentenceDetector implements SentenceDetector {
		@Override
		public String[] sentDetect(String text) {
			if (text == null || text.isEmpty()) {
				return new String[0];
			}
			// Split on common sentence terminators while preserving the delimiter with the sentence.
			return text.split("(?<=[.!?])\\s+");
		}

		@Override
		public Span[] sentPosDetect(String text) {
			String[] sentences = sentDetect(text);
			Span[] spans = new Span[sentences.length];
			int cursor = 0;
			for (int i = 0; i < sentences.length; i++) {
				String s = sentences[i];
				int start = text.indexOf(s, cursor);
				if (start < 0) {
					start = cursor;
				}
				spans[i] = new Span(start, start + s.length());
				cursor = start + s.length();
			}
			return spans;
		}
	}
}
