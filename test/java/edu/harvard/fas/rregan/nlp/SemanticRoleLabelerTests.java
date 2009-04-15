/*
 * $Id: SemanticRoleLabelerTests.java,v 1.12 2009/02/12 02:21:17 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp;

import java.util.Map;

import edu.harvard.fas.rregan.AbstractIntegrationTestCase;
import edu.harvard.fas.rregan.nlp.dictionary.Sense;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetFrameRef;
import edu.harvard.fas.rregan.nlp.impl.ConstituentTreePrinter;
import edu.harvard.fas.rregan.nlp.impl.DependencyPrinter;
import edu.harvard.fas.rregan.nlp.impl.StringNLPTextWalker;
import edu.harvard.fas.rregan.nlp.impl.srl.SemanticRoleCollector;
import edu.harvard.fas.rregan.nlp.impl.srl.SemanticRoleCollectorFunction;
import edu.harvard.fas.rregan.nlp.impl.srl.SemanticRolePrinter;

/**
 * @author ron
 */
public class SemanticRoleLabelerTests extends AbstractIntegrationTestCase {

	/**
	 * @param name
	 */
	public SemanticRoleLabelerTests() {
		super();
	}

	/**
	 *
	 */
	public void testProcessing1() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "The user enters some information.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);

		// fails: wrong sense "enters#1" instead of "enters#8"
		Sense sense = getDictionaryRepository().findSensesByLemmaAndSynsetId("enter", 201421622L);
		for (VerbNetFrameRef frameRef : sense.getVerbNetFrameRefs()) {
			log.info(frameRef);
		}
		for (NLPText word : text.getLeaves()) {
			if ("enters".equals(word.getText())) {
				word.setDictionaryWordSense(sense);
			}
		}
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("enters", text.getPrimaryVerb().getText());
		assertEquals("The user", roles.get(SemanticRole.AGENT).getText());
		assertEquals("some information", roles.get(SemanticRole.THEME).getText());
	}

	/**
	 * 
	 */
	public void testProcessing1a() throws Exception {
		try {
			NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
					.getSemanticRoleLabeler();
			String sentence = "The user entered some information in the search form.";
			NLPText text = process(sentence);
			printConstituentParseTree(text);
			printDependencies(text);
			// fails: wrong sense "enters#1" instead of "enters#8"
			for (NLPText word : text.getLeaves()) {
				if ("entered".equals(word.getText())) {
					word.setDictionaryWordSense(getDictionaryRepository()
							.findSensesByLemmaAndSynsetId("enter", 201421622L));
				}
			}
			semanticRoleLabeler.process(text);
			printSemanticRoles(text);
			Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
			assertEquals("entered", text.getPrimaryVerb().getText());
			assertEquals("The user", roles.get(SemanticRole.AGENT).getText());
			assertEquals("some information", roles.get(SemanticRole.THEME).getText());
			assertEquals("the search form", roles.get(SemanticRole.INSTRUMENT).getText());
		} catch (Exception e) {
			log.error(e, e);
			throw e;
		}
	}

	/**
	 * 
	 */
	public void testProcessing1b() throws Exception {
		try {
			NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
					.getSemanticRoleLabeler();
			String sentence = "Some information was entered by the user.";
			NLPText text = process(sentence);
			printConstituentParseTree(text);
			printDependencies(text);
			// fails: wrong sense "enters#1" instead of "enters#8"
			for (NLPText word : text.getLeaves()) {
				if ("entered".equals(word.getText())) {
					word.setDictionaryWordSense(getDictionaryRepository()
							.findSensesByLemmaAndSynsetId("enter", 201421622L));
				}
			}
			semanticRoleLabeler.process(text);
			printSemanticRoles(text);
			Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
			assertEquals("entered", text.getPrimaryVerb().getText());
			assertEquals("the user", roles.get(SemanticRole.AGENT).getText());
			assertEquals("Some information", roles.get(SemanticRole.THEME).getText());
		} catch (Exception e) {
			log.error(e, e);
			throw e;
		}
	}

	/**
	 * 
	 */
	public void testProcessing1c() throws Exception {
		try {
			NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
					.getSemanticRoleLabeler();
			String sentence = "Some information has been entered by the user.";
			NLPText text = process(sentence);
			printConstituentParseTree(text);
			printDependencies(text);
			// fails: wrong sense "enters#1" instead of "enters#8"
			for (NLPText word : text.getLeaves()) {
				if ("entered".equals(word.getText())) {
					word.setDictionaryWordSense(getDictionaryRepository()
							.findSensesByLemmaAndSynsetId("enter", 201421622L));
				}
			}
			semanticRoleLabeler.process(text);
			printSemanticRoles(text);
			Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
			assertEquals("entered", text.getPrimaryVerb().getText());
			assertEquals("the user", roles.get(SemanticRole.AGENT).getText());
			assertEquals("Some information", roles.get(SemanticRole.THEME).getText());
		} catch (Exception e) {
			log.error(e, e);
			throw e;
		}
	}

	/**
	 *
	 */
	public void testProcessing2() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "The phone company billed John for eleven dollars.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		// fails: wrong sense "dollars#1" instead of "dollars#2"
		for (NLPText word : text.getLeaves()) {
			if ("dollars".equals(word.getText())) {
				word.setDictionaryWordSense(getDictionaryRepository().findSensesByLemmaAndSynsetId(
						"dollar", 113395897L));
			}
		}
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("billed", text.getPrimaryVerb().getText());
		assertEquals("The phone company", roles.get(SemanticRole.AGENT).getText());
		assertEquals("John", roles.get(SemanticRole.RECIPIENT).getText());
		assertEquals("eleven dollars", roles.get(SemanticRole.ASSET).getText());
	}

	/**
	 *
	 */
	public void testProcessing2a() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "The phone company has billed John for eleven dollars.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		// fails: wrong sense "dollars#1" instead of "dollars#2"
		for (NLPText word : text.getLeaves()) {
			if ("dollars".equals(word.getText())) {
				word.setDictionaryWordSense(getDictionaryRepository().findSensesByLemmaAndSynsetId(
						"dollar", 113395897L));
			}
		}
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("billed", text.getPrimaryVerb().getText());
		assertEquals("The phone company", roles.get(SemanticRole.AGENT).getText());
		assertEquals("John", roles.get(SemanticRole.RECIPIENT).getText());
		assertEquals("eleven dollars", roles.get(SemanticRole.ASSET).getText());
	}

	/**
	 *
	 */
	public void testProcessing2b() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "The phone company should have billed John for eleven dollars.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		// fails: wrong sense "dollars#1" instead of "dollars#2"
		for (NLPText word : text.getLeaves()) {
			if ("dollars".equals(word.getText())) {
				word.setDictionaryWordSense(getDictionaryRepository().findSensesByLemmaAndSynsetId(
						"dollar", 113395897L));
			}
		}
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("billed", text.getPrimaryVerb().getText());
		assertEquals("The phone company", roles.get(SemanticRole.AGENT).getText());
		assertEquals("John", roles.get(SemanticRole.RECIPIENT).getText());
		assertEquals("eleven dollars", roles.get(SemanticRole.ASSET).getText());
	}

	/**
	 *
	 */
	public void testProcessing2c() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "The phone company is billing John for eleven dollars.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		// fails: wrong sense "dollars#1" instead of "dollars#2"
		for (NLPText word : text.getLeaves()) {
			if ("dollars".equals(word.getText())) {
				word.setDictionaryWordSense(getDictionaryRepository().findSensesByLemmaAndSynsetId(
						"dollar", 113395897L));
			}
		}
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("billing", text.getPrimaryVerb().getText());
		assertEquals("The phone company", roles.get(SemanticRole.AGENT).getText());
		assertEquals("John", roles.get(SemanticRole.RECIPIENT).getText());
		assertEquals("eleven dollars", roles.get(SemanticRole.ASSET).getText());
	}

	/**
	 *
	 */
	public void testProcessing3() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "A user searches for artists by name or genre.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("searches", text.getPrimaryVerb().getText());
		assertEquals("A user", roles.get(SemanticRole.AGENT).getText());
		assertEquals("artists", roles.get(SemanticRole.THEME).getText());
		assertEquals("name or genre", roles.get(SemanticRole.ATTRIBUTE).getText());
	}

	/**
	 *
	 */
	public void testProcessing4() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "new content must be distinguished from archive content with a visual marker.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("distinguished", text.getPrimaryVerb().getText());
		assertEquals("a visual marker", roles.get(SemanticRole.INSTRUMENT).getText());
		assertEquals("new content", roles.get(SemanticRole.THEME1).getText());
		assertEquals("archive content", roles.get(SemanticRole.THEME2).getText());
	}

	/**
	 * 
	 */
	public void testProcessing5() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "new content is distinguished from archive content by a visual marker.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		semanticRoleLabeler.process(text);
		printDependencies(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("distinguished", text.getPrimaryVerb().getText());
		assertEquals("a visual marker", roles.get(SemanticRole.INSTRUMENT).getText());
		assertEquals("new content", roles.get(SemanticRole.THEME).getText());
		assertEquals("archive content", roles.get(SemanticRole.THEME).getText());
	}

	/**
	 *
	 */
	public void testProcessing6() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "I bet Bob a dollar that Pi is a number.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("bet", text.getPrimaryVerb().getText());
		assertEquals("I", roles.get(SemanticRole.ACTOR).getText());
		assertEquals("a dollar", roles.get(SemanticRole.THEME).getText());
		assertEquals("Bob", roles.get(SemanticRole.ACTOR).getText());
	}

	/**
	 *
	 */
	public void testProcessing8() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "What she said makes sense.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("said", text.getPrimaryVerb().getText());
		assertEquals("she", roles.get(SemanticRole.AGENT).getText());
		assertEquals("What", roles.get(SemanticRole.THEME).getText());
		assertEquals("What", roles.get(SemanticRole.PROPOSITION).getText());
	}

	/**
	 *
	 */
	public void testProcessing9() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "The user creates a new project.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("creates", text.getPrimaryVerb().getText());
		assertEquals("The user", roles.get(SemanticRole.AGENT).getText());
		assertEquals("a new project", roles.get(SemanticRole.PRODUCT).getText());
	}

	/**
	 *
	 */
	public void testProcessing10() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "The user is authorized to create a new project.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("authorized", text.getPrimaryVerb().getText());
		assertEquals("The user", roles.get(SemanticRole.BENEFICIARY).getText());
		assertEquals("create a new project.", roles.get(SemanticRole.TOPIC).getText());
	}

	/**
	 *
	 */
	public void testProcessing11() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "The system verifies the user is authorized to create a new project.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("verifies", text.getPrimaryVerb().getText());
		assertEquals("The system", roles.get(SemanticRole.AGENT).getText());
		assertEquals("the user", roles.get(SemanticRole.PROPOSITION).getText());
	}

	/**
	 *
	 */
	public void testProcessingPrepositionalObject() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "I sat on the chair.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("sat", text.getPrimaryVerb().getText());
		assertEquals("I", roles.get(SemanticRole.AGENT).getText());
		assertEquals("the chair", roles.get(SemanticRole.LOCATION).getText());
	}

	/**
	 * The semantic role labeler reverses the passive subject and direct object
	 * of a passive verb. For example:<br>
	 * The man has been killed by the police. -> The police killed the man.
	 */
	public void testProcessingSubjectAsPrepObjectOfAPassiveVerb() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "The man has been killed by the police.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("killed", text.getPrimaryVerb().getText());
		assertEquals("the police", roles.get(SemanticRole.AGENT).getText());
		assertEquals("The man", roles.get(SemanticRole.PATIENT).getText());
	}

	/**
	 * http://www.ucl.ac.uk/internet-grammar/function/inobj.htm
	 */
	public void testProcessingIndirectObject1() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "She gave me a raise.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("gave", text.getPrimaryVerb().getText());
		assertEquals("She", roles.get(SemanticRole.AGENT).getText());
		assertEquals("a raise", roles.get(SemanticRole.THEME).getText());
		assertEquals("me", roles.get(SemanticRole.PATIENT).getText());
	}

	/**
	 * 
	 */
	public void testProcessingIndirectObject2() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "John sent Sarah flowers.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("sent", text.getPrimaryVerb().getText());
		assertEquals("John", roles.get(SemanticRole.AGENT).getText());
		assertEquals("flowers", roles.get(SemanticRole.THEME).getText());
		assertEquals("Sarah", roles.get(SemanticRole.RECIPIENT).getText());
	}

	/**
	 * 
	 */
	public void testProcessingIndirectObject3() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "The crowd cheered the actor upon entering the hall.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("cheered", text.getPrimaryVerb().getText());
		assertEquals("The crowd", roles.get(SemanticRole.AGENT).getText());
		assertEquals("entering the hall", roles.get(SemanticRole.TOPIC).getText());
		assertEquals("the actor", roles.get(SemanticRole.BENEFICIARY).getText());
	}

	/**
	 * 
	 */
	public void testProcessingOpenClausalComplement() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		String sentence = "John hates eating fish.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("hates", text.getPrimaryVerb().getText());
		assertEquals("John", roles.get(SemanticRole.AGENT).getText());
		assertEquals("eating fish", roles.get(SemanticRole.TOPIC).getText());
	}

	/**
	 * 
	 */
	public void testProcessingVerbNetExample_name_NP_NP() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		// <NP value="Agent"><SYNRESTRS/></NP><VERB/><NP
		// value="Theme"><SYNRESTRS/></NP><NP
		// value="Predicate"><SYNRESTRS/></NP>
		String sentence = "The captain named the ship Seafarer.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("named", text.getPrimaryVerb().getText());
		assertEquals("The captain", roles.get(SemanticRole.AGENT).getText());
		assertEquals("the ship", roles.get(SemanticRole.BENEFICIARY).getText());
		assertNull("Seafarer", roles.get(SemanticRole.ATTRIBUTE).getText());
	}

	/**
	 * 
	 */
	public void testProcessingVerbNetExample_increase_NP_ADJP_PP() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		// <NP value="Patient"><SYNRESTRS/></NP><VERB/><ADJ/><PREP
		// value="with"><SELRESTRS/></PREP><NP
		// value="Instrument"><SYNRESTRS/></NP>
		String sentence = "Piggy banks break open with a hammer.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("break", text.getPrimaryVerb().getText());
		assertEquals("a hammer", roles.get(SemanticRole.INSTRUMENT).getText());
		assertEquals("Piggy banks", roles.get(SemanticRole.PATIENT).getText());
	}

	/**
	 * 
	 */
	public void testProcessingVerbNetExample_break_NP_NP_ADJP_PP() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		// <NP value="Agent"><SYNRESTRS/></NP><VERB/><NP
		// value="Patient"><SYNRESTRS/></NP><ADJ/><PREP
		// value="with"><SELRESTRS/></PREP><NP
		// value="Instrument"><SYNRESTRS/></NP>
		String sentence = "Tony broke the piggy bank open with a hammer.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("broke", text.getPrimaryVerb().getText());
		assertEquals("Tony", roles.get(SemanticRole.AGENT).getText());
		assertEquals("Piggy banks", roles.get(SemanticRole.PATIENT).getText());
		assertEquals("a hammer", roles.get(SemanticRole.INSTRUMENT).getText());
	}

	/**
	 * 
	 */
	public void testProcessingVerbNetExample_revolt_NP_NP_ADJ() {
		NLPProcessor<NLPText> semanticRoleLabeler = getNlpProcessorFactory()
				.getSemanticRoleLabeler();
		// <NP value="Cause"><SYNRESTRS/></NP><VERB/><NP
		// value="Experiencer"><SYNRESTRS/></NP><ADJ/>
		String sentence = "That movie bored me silly.";
		NLPText text = process(sentence);
		printConstituentParseTree(text);
		printDependencies(text);
		semanticRoleLabeler.process(text);
		printSemanticRoles(text);
		Map<SemanticRole, NLPText> roles = collectSemanticRoles(text);
		assertEquals("bored", text.getPrimaryVerb().getText());
		assertEquals("That movie", roles.get(SemanticRole.CAUSE).getText());
		assertEquals("me", roles.get(SemanticRole.EXPERIENCER).getText());
	}

	/**
	 * Break the supplied string into sentences, parse and disambiguate it.
	 * 
	 * @param string -
	 *            a string of text.
	 * @return the NLPText representation of the original string.
	 */
	private NLPText process(String text) {
		NLPText nlpText = getNlpProcessorFactory().createNLPText(text);
		getNlpProcessorFactory().getSentencizer().process(nlpText);
		getNlpProcessorFactory().getParser().process(nlpText);
		getNlpProcessorFactory().getPrimaryVerbFinder().process(nlpText);
		getNlpProcessorFactory().getLemmatizer().process(nlpText);
		getNlpProcessorFactory().getDictionizer().process(nlpText);
		getNlpProcessorFactory().getNamedEntityResolver().process(nlpText);
		getNlpProcessorFactory().getWordSenseDisambiguator().process(nlpText);

		return nlpText;
	}

	private void printConstituentParseTree(NLPText text) {
		System.out.println(getNlpProcessorFactory().getConstituentTreePrinter().process(text));
	}

	private void printDependencies(NLPText text) {
		System.out.println(getNlpProcessorFactory().getDependencyPrinter().process(text));
	}

	private void printSemanticRoles(NLPText text) {
		System.out.println(getNlpProcessorFactory().getSemanticRolePrinter().process(text));
	}

	private Map<SemanticRole, NLPText> collectSemanticRoles(NLPText text) {
		return new SemanticRoleCollector(new SemanticRoleCollectorFunction(text.getPrimaryVerb()))
				.process(text);
	}

}
