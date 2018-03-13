package com.rreganjr.nlp;

import com.rreganjr.TestCase;
import com.rreganjr.nlp.impl.ConstituentTreePrinter;
import com.rreganjr.nlp.impl.NLPTextImpl;
import com.rreganjr.nlp.impl.Sentencizer;
import com.rreganjr.nlp.impl.StanfordLexicalizedParser;
import com.rreganjr.nlp.impl.StringNLPTextWalker;

public class NLPConstituentParseTests extends TestCase {

	private final NLPProcessor<NLPText> parser = new StanfordLexicalizedParser();
	private final NLPProcessor<NLPText> sentencizer = new Sentencizer();
	private final StringNLPTextWalker treePrinter = new StringNLPTextWalker(
			new ConstituentTreePrinter(500, true));

	private NLPText process(String sentence) {
		NLPText text = new NLPTextImpl(sentence);
		sentencizer.process(text);
		parser.process(text);
		return text;
	}

	public void test1() {
		String sentence = "Users will access the Streaming Video service via a link from the Virgin XL WAP environment.";
		String expectedParseTree = "(ROOT (S (NP (NNS Users)) (VP (MD will) (VP (VB access) (NP (NP (DT the) (NNP Streaming) (NNP Video) (NN service)) (PP (IN via) (NP (DT a) (NN link)))) (PP (IN from) (NP (DT the) (NNP Virgin) (NNP XL) (NNP WAP) (NN environment))))) (. .)))";
		NLPText text = process(sentence);
		String actualParseTree = treePrinter.process(text);
		System.out.println(actualParseTree);
		assertEqualsIgnoreWhitespace(expectedParseTree, actualParseTree);
	}

	public void test2() {
		String sentence = "The EVDO Video Product must allow the user to refer content to a friend via SMS.";
		String expectedParseTree = "(ROOT (S (NP (DT The) (NNP EVDO) (NNP Video) (NNP Product)) (VP (MD must) (VP (VB allow) (NP (DT the) (NN user) (S (VP (TO to) (VP (VB refer) (NP (NN content)) (PP (TO to) (NP (NP (DT a) (NN friend)) (PP (IN via) (NP (NNP SMS))))))))))) (. .)))";
		NLPText text = process(sentence);
		String actualParseTree = treePrinter.process(text);
		System.out.println(actualParseTree);
		assertEqualsIgnoreWhitespace(expectedParseTree, actualParseTree);
	}

	public void test3() {
		String sentence = "The EVDO Streaming Video Main Menu should contain: Featured Content, VMU Channels: Life & Hot Shots, Catalog Browse, Top Rated Content in the Community, Top Viewed Content in the Community, VMU-branded content channels that include the Nellymoser Sourced Content.";
		String expectedParseTree = "(ROOT (S (NP (DT The) (NNP EVDO) (NNP Streaming) (NNP Video) (NNP Main) (NNP Menu)) (VP (MD should) (VP (VB contain) (: :) (NP (NP (NP (NNP Featured) (NNP Content)) (, ,) (NP (NNP VMU) (NNP Channels))) (: :) (NP (NP (NNP Life) (CC &) (NNP Hot) (NNP Shots)) (, ,) (NP (NNP Catalog) (NNP Browse))) (, ,) (NP (NP (JJ Top) (NNP Rated) (NNP Content)) (PP (IN in) (NP (DT the) (NNP Community)))) (, ,) (NP (NP (NNP Top) (NNP Viewed) (NNP Content)) (PP (IN in) (NP (DT the) (NNP Community)))) (, ,) (NP (NP (JJ VMU-branded) (NN content) (NNS channels)) (SBAR (WHNP (WDT that)) (S (VP (VBP include) (NP (DT the) (NNP Nellymoser) (NNP Sourced) (NNP Content))))))))) (. .)))";
		NLPText text = process(sentence);
		String actualParseTree = treePrinter.process(text);
		System.out.println(actualParseTree);
		assertEqualsIgnoreWhitespace(expectedParseTree, actualParseTree);
	}

	public void test4() {
		String sentence = "The EVDO Streaming Video Main Menu user flow is: Main Menu -> Content Folder or Browse Results Page -> Video Clip Details Page -> Purchase Confirmation (If applicable) -> Stream -> Video Clip Return Page -> Originating Content Folder or Browse Results Page.";
		String expectedParseTree = "(ROOT (S (NP (DT The) (NNP EVDO) (NNP Streaming) (NNP Video) (NNP Main) (NNP Menu) (NN user) (NN flow)) (VP (VBZ is) (: :) (NP (NP (NNP Main) (NNP Menu) (NNP ->) (NNP Content) (NNP Folder) (CC or) (NNP Browse) (NNS Results)) (NP (NP (NNP Page) (NNP ->) (NNP Video) (NNP Clip) (NNP Details) (NNP Page) (NNP ->) (NNP Purchase) (NNP Confirmation)) (PRN (, () (SBAR (IN If) (FRAG (ADJP (JJ applicable)))) (, )))) (CC ->) (NP (NP (NNP Stream) (NNP ->) (NNP Video) (NNP Clip) (NNP Return) (NNP Page) (NNP ->) (NNP Originating) (NNP Content) (NNP Folder) (CC or) (NNP Browse) (NNS Results)) (NP (NNP Page))))) (. .)))";
		NLPText text = process(sentence);
		String actualParseTree = treePrinter.process(text);
		System.out.println(actualParseTree);
		assertEqualsIgnoreWhitespace(expectedParseTree, actualParseTree);
	}

	public void test5() {
		String sentence = "The Video Clip Details Page will include user Poll Results, Community Rating and User Comments.";
		String expectedParseTree = "(ROOT (S (NP (DT The) (NNP Video) (NNP Clip) (NNP Details) (NNP Page)) (VP (MD will) (VP (VB include) (NP (NP (NN user) (NN Poll) (NNS Results)) (, ,) (NP (NNP Community) (NNP Rating)) (CC and) (NP (NNP User) (NNP Comments))))) (. .)))";
		NLPText text = process(sentence);
		String actualParseTree = treePrinter.process(text);
		System.out.println(actualParseTree);
		assertEqualsIgnoreWhitespace(expectedParseTree, actualParseTree);
	}

	public void test6() {
		String sentence = "The Video Clip Return Page will allow the user to rate the content, answer a poll question and enter a comment.";
		String expectedParseTree = "(ROOT (S (NP (DT The) (NNP Video) (NNP Clip) (NNP Return) (NNP Page)) (VP (MD will) (VP (VP (VB allow) (NP (NP (DT the) (NN user)) (PP (TO to) (NP (NN rate)))) (NP (DT the) (NN content))) (, ,) (VP (VB answer) (NP (DT a) (NN poll) (NN question))) (CC and) (VP (VB enter) (NP (DT a) (NN comment))))) (. .)))";
		NLPText text = process(sentence);
		String actualParseTree = treePrinter.process(text);
		System.out.println(actualParseTree);
		assertEqualsIgnoreWhitespace(expectedParseTree, actualParseTree);
	}

	public void test7() {
		String sentence = "\"New\" Content must be distinguished from \"archive\" content with a tag or other visual marker";
		String expectedParseTree = "(ROOT (S (NP (X) (NNP New) (X) (NNP Content)) (VP (MD must) (VP (VB be) (ADJP (JJ distinguished) (PP (IN from) (NP (X) (JJ archive) (X) (NN content)))) (PP (IN with) (NP (NP (DT a) (NN tag)) (CC or) (NP (JJ other) (JJ visual) (NN marker))))))))";
		NLPText text = process(sentence);
		String actualParseTree = treePrinter.process(text);
		System.out.println(actualParseTree);
		assertEqualsIgnoreWhitespace(expectedParseTree, actualParseTree);
	}

	public void test8() {
		String sentence = "A Free video gallery should be made available for users to preview the EVDO Streaming Video product.";
		String expectedParseTree = "(ROOT (S (NP (DT A) (JJ Free) (NN video) (NN gallery)) (VP (MD should) (VP (VB be) (VP (VBN made) (S (ADJP (JJ available))) (PP (IN for) (NP (NNS users))) (VP (TO to) (VP (VB preview) (NP (DT the) (NNP EVDO) (NNP Streaming) (NNP Video) (NN product))))))) (. .)))";
		NLPText text = process(sentence);
		String actualParseTree = treePrinter.process(text);
		System.out.println(actualParseTree);
		assertEqualsIgnoreWhitespace(expectedParseTree, actualParseTree);
	}

	public void test9() {
		String sentence = "The EVDO Video Product must support \"head to head\" matchups that allow users to vote between two pieces of content";
		String expectedParseTree = "(ROOT (S (NP (DT The) (NNP EVDO) (NNP Video) (NNP Product)) (VP (MD must) (VP (VB support) (X) (ADVP (NN head) (TO to) (NN head)) (X) (NP (NP (NNS matchups)) (SBAR (WHNP (WDT that)) (S (VP (VBP allow) (S (NP (NNS users)) (VP (TO to) (VP (VB vote) (PP (IN between) (NP (NP (CD two) (NNS pieces)) (PP (IN of) (NP (NN content))))))))))))))))";
		NLPText text = process(sentence);
		String actualParseTree = treePrinter.process(text);
		System.out.println(actualParseTree);
		assertEqualsIgnoreWhitespace(expectedParseTree, actualParseTree);
	}
}
