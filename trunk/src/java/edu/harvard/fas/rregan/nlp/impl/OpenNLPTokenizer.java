/*
 * $Id: OpenNLPTokenizer.java,v 1.4 2009/04/01 09:47:02 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
