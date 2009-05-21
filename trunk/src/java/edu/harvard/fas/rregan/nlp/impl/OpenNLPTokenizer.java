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

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import edu.harvard.fas.rregan.ApplicationException;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPText;

/**
 * @author ron
 */
// @Component("openNLPTokenizer")
public class OpenNLPTokenizer extends AbstractOpenNLPTool<NLPText> {

	/**
	 * The name of the property in the NLPImpl.properties file that contains the
	 * path to the open nlp tokenizer model file relative to the classpath.
	 * 
	 * @see PROP_ENGLISH_TOKENIZER_MODEL_FILE_DEFAULT for the default location
	 *      of the file
	 */
	public static final String PROP_ENGLISH_TOKENIZER_MODEL_FILE = "EnglishTokenizerModelFile";

	/**
	 * The default path to the tokenizer model file
	 */
	public static final String PROP_ENGLISH_TOKENIZER_MODEL_FILE_DEFAULT = "resources/nlp/opennlp-tools/EnglishTok.bin.gz";

	private static Tokenizer tokenizer;
	static {
		try {
			ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
					OpenNLPParser.class.getName());

			String modelFile = resourceBundleHelper.getString(PROP_ENGLISH_TOKENIZER_MODEL_FILE,
					PROP_ENGLISH_TOKENIZER_MODEL_FILE_DEFAULT);

			tokenizer = new TokenizerME(readGISModel(modelFile));
		} catch (Exception e) {
			tokenizer = null;
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * create a new tokenizer, initializing if needed.
	 * 
	 * @throws ApplicationException
	 *             if the underlying OpenNLP tokenizer fails to initalize
	 */
	public OpenNLPTokenizer() {
		init();
	}

	private synchronized void init() {
		if (tokenizer == null) {
			try {
				ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
						OpenNLPParser.class.getName());

				String modelFile = resourceBundleHelper.getString(
						PROP_ENGLISH_TOKENIZER_MODEL_FILE,
						PROP_ENGLISH_TOKENIZER_MODEL_FILE_DEFAULT);

				tokenizer = new TokenizerME(readGISModel(modelFile));
			} catch (Exception e) {
				tokenizer = null;
				throw ApplicationException.failedToInitializeComponent(getClass(), e);
			}
		}
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.NLPProcessor#process(edu.harvard.fas.rregan.nlp.NLPText)
	 */
	@Override
	public NLPText process(NLPText text) {
		if (text.is(GrammaticalStructureLevel.SENTENCE)
				|| text.is(GrammaticalStructureLevel.UNKNOWN)) {
			if (text.getLeaves().isEmpty()) {
				for (String word : tokenizer.tokenize(text.getText())) {
					text.getChildren().add(new NLPTextImpl(word, GrammaticalStructureLevel.WORD));
				}
			}
		}
		return text;
	}

	protected static Tokenizer getTokenizer() {
		return tokenizer;
	}
}
