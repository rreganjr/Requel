package com.rreganjr.nlp;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.rreganjr.TestCase;
import com.rreganjr.nlp.impl.NLPTextImpl;
import com.rreganjr.nlp.impl.OpenNLPTagger;
import com.rreganjr.nlp.impl.OpenNLPTokenizer;
import com.rreganjr.nlp.impl.Sentencizer;
import com.rreganjr.nlp.impl.StanfordLexicalizedParser;

public class NLPTests extends TestCase {

	private final NLPProcessor<NLPText> parser = new StanfordLexicalizedParser();
	private final NLPProcessor<NLPText> sentencizer = new Sentencizer();
	private final NLPProcessor<NLPText> tokenizer = new OpenNLPTokenizer();
	private final NLPProcessor<NLPText> posTagger = new OpenNLPTagger();

	private NLPText process(String sentence) {
		NLPText text = new NLPTextImpl(sentence);
		sentencizer.process(text);
		parser.process(text);
		return text;
	}

	public void testSentencize() {
		List<String> expectedSentences = Arrays
				.asList(new String[] { "This is the first sentence.", "This is the second.",
						"This is Mr. Bob's sentence.", "This sentence cost $3.50.",
						"Pi is 3.1415 and e is 2.7 something.", "This is a question?",
						"The last sentence!" });
		StringBuilder sb = new StringBuilder();
		for (String sentence : expectedSentences) {
			sb.append(sentence);
			sb.append(" ");
		}
		String sentences = sb.toString();
		int index = 0;
		NLPText actualSentences = sentencizer.process(new NLPTextImpl(sentences));
		for (String expectedSentence : expectedSentences) {
			assertEquals(expectedSentence, actualSentences.getChildren().get(index).getText());
			assertEquals(GrammaticalStructureLevel.SENTENCE, actualSentences.getChildren().get(
					index++).getGrammaticalStructureLevel());
		}
	}

	public void testTokenize() {
		List<String> expectedTokens = Arrays.asList(new String[] { "I", "bet", "Bob", "$", "1.25",
				"that", "Pi", "is", "3.1415", "and", "e", "is", "2.7", "something", "." });
		StringBuilder sb = new StringBuilder();
		String lastToken = "";
		for (String token : expectedTokens) {
			if (!token.equals(".") && !lastToken.equals("$")) {
				sb.append(" ");
			}
			sb.append(token);
			lastToken = token;
		}
		String text = sb.toString();
		int index = 0;
		NLPText actualTokens = tokenizer.process(new NLPTextImpl(text));
		for (String expectedToken : expectedTokens) {
			assertEquals(expectedToken, actualTokens.getChildren().get(index).getText());
			assertEquals(GrammaticalStructureLevel.WORD, actualTokens.getChildren().get(index++)
					.getGrammaticalStructureLevel());
		}
	}

	public void testPOSTagging() {
		List<ParseTag> expectedTags = Arrays.asList(new ParseTag[] { ParseTag.PRP, // I
				ParseTag.VBP, // bet
				ParseTag.NNP, // Bob
				ParseTag.PUNC_DOLLAR, // $
				ParseTag.CD, // 1.25
				ParseTag.DT, // that
				ParseTag.NNP, // Pi
				ParseTag.VBZ, // is
				ParseTag.CD, // 3.1415
				ParseTag.CC, // and
				ParseTag.NN, // e (should be NNP?)
				ParseTag.VBZ, // is
				ParseTag.CD, // 2.7
				ParseTag.NN, // something
				ParseTag.PUNC_TERMINATOR // .
				});
		String sentence = "I bet Bob $1.25 that Pi is 3.1415 and e is 2.7 something.";
		NLPText actualTags = posTagger.process(new NLPTextImpl(sentence));
		assertEquals(expectedTags.size(), actualTags.getChildren().size());
		int index = 0;
		for (ParseTag expectedTag : expectedTags) {
			assertEquals(expectedTag, actualTags.getChildren().get(index).getText());
			assertEquals(GrammaticalStructureLevel.WORD, actualTags.getChildren().get(index++)
					.getGrammaticalStructureLevel());
		}
		assertEquals(expectedTags, actualTags);
	}

	// TODO: move to dictionary tests
	public void testSimilarWords() {
		Set<String> expectedWords = new TreeSet<String>();
		expectedWords.addAll(Arrays.asList(new String[] { "bread", "break", "bream", "cap", "chap",
				"cheat", "cheep", "clap", "clean", "clear", "cleat", "crab", "cram", "cramp",
				"crap", "creak", "cream", "creep", "reap" }));
	}
}
