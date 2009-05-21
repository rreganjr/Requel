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

import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Pattern;

import opennlp.maxent.MaxentModel;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.lang.english.HeadRules;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.ParserChunker;
import opennlp.tools.parser.ParserME;
import opennlp.tools.util.Sequence;
import opennlp.tools.util.Span;
import edu.harvard.fas.rregan.ApplicationException;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.ParseTag;

/**
 * @author ron
 */
// @Component("openNLPParser")
public class OpenNLPParser extends OpenNLPTagger {

	/**
	 * The name of the property in the NLPImpl.properties file that contains the
	 * path to the open nlp parser head rules file relative to the classpath.
	 * 
	 * @see PROP_PARSER_HEAD_RULES_FILE_DEFAULT for the default location of the
	 *      file
	 */
	public static final String PROP_PARSER_HEAD_RULES_FILE = "HeadRulesFile";

	/**
	 * The default path to the parser head rules file
	 */
	public static final String PROP_PARSER_HEAD_RULES_FILE_DEFAULT = "resources/nlp/opennlp-tools/parser/head_rules";

	/**
	 * The name of the property in the NLPImpl.properties file that contains the
	 * path to the open nlp parser chunker model file relative to the classpath.
	 * 
	 * @see PROP_PARSER_CHUNKER_MODEL_FILE_DEFAULT for the default location of
	 *      the file
	 */
	public static final String PROP_PARSER_CHUNKER_MODEL_FILE = "ParserChunkerModelFile";

	/**
	 * The default path to the chunker model file
	 */
	public static final String PROP_PARSER_CHUNKER_MODEL_FILE_DEFAULT = "resources/nlp/opennlp-tools/parser/chunk.bin.gz";

	/**
	 * The name of the property in the NLPImpl.properties file that contains the
	 * path to the open nlp parser build model file relative to the classpath.
	 * 
	 * @see PROP_PARSER_BUILD_MODEL_FILE_DEFAULT for the default location of the
	 *      file
	 */
	public static final String PROP_PARSER_BUILD_MODEL_FILE = "ParserBuildModelFile";

	/**
	 * The default path to the parser build model file
	 */
	public static final String PROP_PARSER_BUILD_MODEL_FILE_DEFAULT = "resources/nlp/opennlp-tools/parser/build.bin.gz";

	/**
	 * The name of the property in the NLPImpl.properties file that contains the
	 * path to the open nlp parser build model file relative to the classpath.
	 * 
	 * @see PROP_PARSER_CHECK_MODEL_FILE_DEFAULT for the default location of the
	 *      file
	 */
	public static final String PROP_PARSER_CHECK_MODEL_FILE = "ParserCheckModelFile";

	/**
	 * The default path to the parser check model file
	 */
	public static final String PROP_PARSER_CHECK_MODEL_FILE_DEFAULT = "resources/nlp/opennlp-tools/parser/check.bin.gz";

	private static Pattern untokenizedParenPattern1 = Pattern.compile("([^ ])([({)}])");
	private static Pattern untokenizedParenPattern2 = Pattern.compile("([({)}])([^ ])");

	private static ParserME parser;

	/**
	 * create a new parser, initializing if needed.
	 * 
	 * @throws ApplicationException
	 *             if the underlying OpenNLP parser fails to initalize
	 */
	public OpenNLPParser() {
		init();
	}

	private synchronized void init() {
		if (parser == null) {
			try {
				ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
						OpenNLPParser.class.getName());

				String headRulesFile = resourceBundleHelper.getString(PROP_PARSER_HEAD_RULES_FILE,
						PROP_PARSER_HEAD_RULES_FILE_DEFAULT);
				String headRulesPath = URLDecoder.decode(OpenNLPParser.class.getClassLoader()
						.getResource(headRulesFile).getPath(), "utf8");
				HeadRules headRules = new HeadRules(headRulesPath);

				String chunkerModelFile = resourceBundleHelper.getString(
						PROP_PARSER_CHUNKER_MODEL_FILE, PROP_PARSER_CHUNKER_MODEL_FILE_DEFAULT);
				ParserChunker chunker = new Chunker(readGISModel(chunkerModelFile));

				String buildModelFile = resourceBundleHelper.getString(
						PROP_PARSER_BUILD_MODEL_FILE, PROP_PARSER_BUILD_MODEL_FILE_DEFAULT);

				String checkModelFile = resourceBundleHelper.getString(
						PROP_PARSER_CHECK_MODEL_FILE, PROP_PARSER_CHECK_MODEL_FILE_DEFAULT);

				parser = new ParserME(readGISModel(buildModelFile), readGISModel(checkModelFile),
						getTagger(), chunker, headRules);
			} catch (Exception e) {
				parser = null;
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
			Parse parse = null;
			Span[] spans = null;
			String sentence = text.getText();
			if (text.getLeaves().isEmpty()) {
				sentence = untokenizedParenPattern1.matcher(sentence).replaceAll("$1 $2");
				sentence = untokenizedParenPattern2.matcher(sentence).replaceAll("$1 $2");
				spans = getTokenizer().tokenizePos(sentence);
			} else {
				spans = new Span[text.getLeaves().size()];
				int start = 0;
				int end = 0;
				for (NLPText word : text.getLeaves()) {
					start = sentence.indexOf(word.getText(), start);
					end = start + word.getText().length();
					spans[word.getWordIndex()] = new Span(start, end);
				}
			}
			Parse topNode = new Parse(sentence, new Span(0, sentence.length()), "INC", 1, null);
			for (int i = 0; i < spans.length; i++) {
				topNode.insert(new Parse(sentence, spans[i], ParserME.TOK_NODE, 0));
			}
			parse = parser.parse(topNode);
			if (parse != null) {
				copyOpenNLPParseToNLPText((NLPTextImpl) text, parse, new Counter());
			}
		}
		return text;
	}

	private static void copyOpenNLPParseToNLPText(NLPTextImpl parent, Parse parse,
			Counter wordIndexCounter) {

		ParseTag tag = ParseTag.tagOf(parse.getType());

		NLPTextImpl node;
		if (parse.isPosTag() && (parse.getChildCount() == 1)) {
			// word level tags
			String word = parse.getSpan().toString();
			node = new NLPTextImpl(parent, wordIndexCounter.getCount(), word, tag);
			parent.getChildren().add(node);
			wordIndexCounter.incr();
		} else {
			if (ParserME.TOP_NODE.equals(parse.getType())) {
				// skip the root
				parent.setParseTag(ParseTag.ROOT);
				node = parent;
			} else {
				// phrase and clause level tags
				node = new NLPTextImpl(parent, tag);
				parent.getChildren().add(node);
			}
			for (Parse child : parse.getChildren()) {
				copyOpenNLPParseToNLPText(node, child, wordIndexCounter);
			}
		}
	}

	private static class Counter {
		private int count = 0;

		protected void incr() {
			count++;
		}

		protected int getCount() {
			return count;
		}
	}

	private static class Chunker extends ChunkerME implements ParserChunker {

		private Chunker(MaxentModel mod) {
			super(mod);
		}

		public Sequence[] topKSequences(List sentence, List tags) {
			return beam.bestSequences(10, sentence.toArray(), new Object[] { tags });
		}

		public Sequence[] topKSequences(String[] sentence, String[] tags, double minSequenceScore) {
			return beam.bestSequences(10, sentence, new Object[] { tags }, minSequenceScore);
		}
	}
}
