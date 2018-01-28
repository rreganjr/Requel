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
package com.rreganjr.nlp.impl;

import java.io.CharArrayReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import net.sf.echopm.ResourceBundleHelper;
import com.rreganjr.nlp.GrammaticalRelationType;
import com.rreganjr.nlp.GrammaticalStructureLevel;
import com.rreganjr.nlp.NLPProcessor;
import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.ParseTag;
import com.rreganjr.nlp.ParserException;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

/**
 * Wrapper for Stanford Parser.
 * 
 * @author ron
 */
@Component("stanfordParser")
public class StanfordLexicalizedParser implements NLPProcessor<NLPText> {
	private static final Logger log = Logger.getLogger(StanfordLexicalizedParser.class);

	/**
	 * The name of the property in the Parser.properties file that contains the
	 * path to the serialized/zipped parser model relative to the classpath. By
	 * default the path is "nlp/stanford-parser/englishPCFG.ser.gz"
	 */
	public static final String PROP_PARSER_FILE = "StanfordParserFileModel";

	/**
	 * The default parser model
	 */
	public static final String PROP_PARSER_FILE_DEFAULT = "nlp/stanford-parser/englishPCFG.ser.gz";

	/**
	 * The name of the property in the Parser.properties file for the parameters
	 * to pass to the LexicalizedParser. The parameters are expected to be of
	 * the form "-param1 param1value -param2 param2value ..." where each token
	 * is delimited by whitespace. The parameters will be split into a String
	 * array and passed to the parser via the setOptionsFlags. By default no
	 * parameters are supplied to the parser.
	 */
	public static final String PROP_OPTION_FLAGS = "optionFlags";

	private static LexicalizedParser parser;
	private static final TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	private static final GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();

	static {
		try {
			ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
					StanfordLexicalizedParser.class.getName());

			String parserFile = resourceBundleHelper.getString(PROP_PARSER_FILE,
					PROP_PARSER_FILE_DEFAULT);

			// TODO: remove empty option elements
			String[] optionFlags = resourceBundleHelper.getString(PROP_OPTION_FLAGS, "").split(" \t\n");
			parser = new LexicalizedParser(new ObjectInputStream(new GZIPInputStream(
					StanfordLexicalizedParser.class.getClassLoader()
							.getResourceAsStream(parserFile))));
			if ((optionFlags != null) && (optionFlags.length > 0) && (optionFlags[0].length() > 0)) {
				//parser.setOptionFlags(optionFlags);
			}
		} catch (Exception e) {
			log.error("Failed to initialize Parser", e);
			parser = null;
//			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * 
	 */
	public StanfordLexicalizedParser() {
	}

	@Override
	public NLPText process(NLPText text) {
		if (text.hasText()) {
			if (GrammaticalStructureLevel.PARAGRAPH.equals(text.getGrammaticalStructureLevel())) {
				for (NLPText sentence : text.getChildren()) {
					process(sentence);
				}
			} else if (text.is(GrammaticalStructureLevel.SENTENCE)
					&& (text.getChildren().size() == 0)) {
				parse(text);
			}
		}
		return text;
	}

	private static synchronized void parse(NLPText text) throws ParserException {
		if (parser == null) {
			return;
		}

		List<? extends HasWord> tokens = null;
		if (text.getLeaves().isEmpty()) {
			String sentence = prepareSentence(text.getText());
			Tokenizer<? extends HasWord> tokenizer = tlp.getTokenizerFactory().getTokenizer(
					new CharArrayReader(sentence.toCharArray()));
			tokens = tokenizer.tokenize();
		} else {
			List<TaggedWord> xtokens = new ArrayList<TaggedWord>();
			for (NLPText word : text.getLeaves()) {
				if (word.getParseTag() != null) {
					xtokens.add(new TaggedWord(word.getText(), word.getParseTag().getText()));
				} else {
					xtokens.add(new TaggedWord(word.getText()));
				}
			}
			tokens = xtokens;
		}
		Tree stanfordTree = parser.parseTree(tokens);
		if (stanfordTree != null && !stanfordTree.isEmpty()) {
			copyStanfordTreeToNLPText((NLPTextImpl) text, stanfordTree, stanfordTree.getLeaves(), new Counter());

			GrammaticalStructure gs = gsf.newGrammaticalStructure(stanfordTree);
			Collection<TypedDependency> typedDependencies = gs.typedDependencies();
			List<NLPText> leaves = text.getLeaves();
			for (TypedDependency dependency : typedDependencies) {
				NLPTextImpl governor = (NLPTextImpl) leaves.get(dependency.gov().index() - 1);
				NLPTextImpl dependent = (NLPTextImpl) leaves.get(dependency.dep().index() - 1);
				GrammaticalRelationType type = GrammaticalRelationType
						.getGrammaticalRelationByShortName(dependency.reln().getShortName());
				text.getGrammaticalRelations().add(
						new GrammaticalRelationImpl(type, governor, dependent));
			}
		} else {
			throw ParserException.parseFailed();
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

	private static void copyStanfordTreeToNLPText(NLPTextImpl parent, Tree stanfordTree,
			final List<Tree> stanfordLeaves, Counter wordIndexCounter) {

		ParseTag tag = ParseTag.tagOf(stanfordTree.value());

		NLPTextImpl node;
		if (stanfordTree.isPreTerminal()) {
			// word level tags
			String word = stanfordTree.firstChild().value();
			node = new NLPTextImpl(parent, wordIndexCounter.getCount(), word, tag);
			parent.getChildren().add(node);
			wordIndexCounter.incr();
		} else {
			if (ParseTag.ROOT.equals(tag)) {
				// skip the root
				parent.setParseTag(tag);
				node = parent;
			} else {
				// phrase and clause level tags
				node = new NLPTextImpl(parent, tag);
				parent.getChildren().add(node);
			}
			for (Tree child : stanfordTree.children()) {
				copyStanfordTreeToNLPText(node, child, stanfordLeaves, wordIndexCounter);
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
