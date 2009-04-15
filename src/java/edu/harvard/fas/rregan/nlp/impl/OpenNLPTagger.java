/*
 * $Id: OpenNLPTagger.java,v 1.4 2009/04/01 09:47:02 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import opennlp.maxent.MaxentModel;
import opennlp.tools.parser.ParserTagger;
import opennlp.tools.postag.DefaultPOSContextGenerator;
import opennlp.tools.postag.POSContextGenerator;
import opennlp.tools.postag.POSDictionary;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.postag.TagDictionary;
import opennlp.tools.util.Sequence;
import edu.harvard.fas.rregan.ApplicationException;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.ParseTag;

/**
 * @author ron
 */
// @Component("openNLPTagger")
public class OpenNLPTagger extends OpenNLPTokenizer {

	/**
	 * The name of the property in the NLPImpl.properties file that contains the
	 * path to the open nlp part-of-speech tagger tag dictionary relative to the
	 * classpath.
	 * 
	 * @see PROP_POSTAGGER_DICTIONARY_FILE_DEFAULT for the default location of
	 *      the file
	 */
	public static final String PROP_POSTAGGER_DICTIONARY_FILE = "POSTaggerDictionaryFile";

	/**
	 * The default path to the POS tagger dictionary file
	 */
	public static final String PROP_POSTAGGER_DICTIONARY_FILE_DEFAULT = "resources/nlp/opennlp-tools/parser/tagdict";

	/**
	 * The name of the property in the NLPImpl.properties file that contains the
	 * path to the open nlp part-of-speech tagger model file relative to the
	 * classpath.
	 * 
	 * @see PROP_POSTAGGER_MODEL_FILE_DEFAULT for the default location of the
	 *      file.
	 */
	public static final String PROP_POSTAGGER_MODEL_FILE = "POSTaggerModelFile";

	/**
	 * The default path to the POS tagger model file.
	 */
	public static final String PROP_POSTAGGER_MODEL_FILE_DEFAULT = "resources/nlp/opennlp-tools/parser/tag.bin.gz";

	private static Tagger posTagger;
	static {
		try {
			ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
					OpenNLPParser.class.getName());

			String modelFile = resourceBundleHelper.getString(PROP_POSTAGGER_MODEL_FILE,
					PROP_POSTAGGER_MODEL_FILE_DEFAULT);

			String dictFile = resourceBundleHelper.getString(PROP_POSTAGGER_DICTIONARY_FILE,
					PROP_POSTAGGER_DICTIONARY_FILE_DEFAULT);
			InputStream dictInputStream = OpenNLPTagger.class.getClassLoader().getResourceAsStream(
					dictFile);
			TagDictionary dict = new POSDictionary(new BufferedReader(new InputStreamReader(
					dictInputStream)), true);

			posTagger = new Tagger(readGISModel(modelFile), new DefaultPOSContextGenerator(null),
					dict);
		} catch (Exception e) {
			posTagger = null;
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * create a new POS tagger, initializing if needed.
	 * 
	 * @throws ApplicationException
	 *             if the underlying OpenNLP tagger fails to initalize
	 */
	public OpenNLPTagger() {
		init();
	}

	private synchronized void init() {
		if (posTagger == null) {
			try {
				ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
						OpenNLPParser.class.getName());

				String modelFile = resourceBundleHelper.getString(PROP_POSTAGGER_MODEL_FILE,
						PROP_POSTAGGER_MODEL_FILE_DEFAULT);

				String dictFile = resourceBundleHelper.getString(PROP_POSTAGGER_DICTIONARY_FILE,
						PROP_POSTAGGER_DICTIONARY_FILE_DEFAULT);
				InputStream dictInputStream = OpenNLPTagger.class.getClassLoader()
						.getResourceAsStream(dictFile);
				TagDictionary dict = new POSDictionary(new BufferedReader(new InputStreamReader(
						dictInputStream)), true);

				posTagger = new Tagger(readGISModel(modelFile),
						new DefaultPOSContextGenerator(null), dict);
			} catch (Exception e) {
				posTagger = null;
				throw ApplicationException.failedToInitializeComponent(getClass(), e);
			}
		}
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.NLPProcessor#process(edu.harvard.fas.rregan.nlp.NLPText)
	 */
	@Override
	public NLPText process(NLPText text) {
		if (text.is(GrammaticalStructureLevel.SENTENCE)) {
			if (text.getLeaves().isEmpty()) {
				super.process(text);
			}
			List<String> words = new ArrayList<String>();
			for (NLPText word : text.getLeaves()) {
				words.add(word.getText());
			}
			List<String> tags = posTagger.tag(words);
			for (int i = 0; i < tags.size(); i++) {
				((NLPTextImpl) text.getLeaves().get(i)).setParseTag(ParseTag.tagOf(tags.get(i)));
			}
		}
		return text;
	}

	protected static Tagger getTagger() {
		return posTagger;
	}

	protected static class Tagger extends POSTaggerME implements ParserTagger {

		private Tagger(MaxentModel mod, POSContextGenerator cg, TagDictionary dict) {
			super(mod, cg, dict);
		}

		public Sequence[] topKSequences(List sentence) {
			return beam.bestSequences(10, sentence.toArray(), null);
		}

		public Sequence[] topKSequences(String[] sentence) {
			return beam.bestSequences(10, sentence, null);
		}
	}
}
