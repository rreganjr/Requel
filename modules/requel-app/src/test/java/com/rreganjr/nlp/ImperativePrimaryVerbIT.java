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
package com.rreganjr.nlp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.SemanticRole;
import com.rreganjr.nlp.impl.srl.SemanticRoleCollector;
import com.rreganjr.nlp.impl.srl.SemanticRoleCollectorFunction;

/**
 * Issue #349: a command has a primary verb - the base-form verb at the root of its parse - and is
 * marked imperative, so a caller can tell "no actor stated" from "no verb found". With the real
 * parser, on the issue's examples.
 */
public class ImperativePrimaryVerbIT extends AbstractIntegrationTestCase {

	@BeforeEach
	void dictionary() throws Exception {
		ensureDictionaryLoaded();
	}

	private NLPText process(String text) {
		return getNlpProcessorFactory().processText(text);
	}

	@Test
	void aCommandHasItsLeadingVerbAsThePrimaryVerbAndIsImperative() {
		for (String[] example : new String[][] { { "Create the room", "Create" },
				{ "Start the livestream", "Start" }, { "Hand the stream key to Zoom", "Hand" } }) {
			NLPText text = process(example[0]);
			assertNotNull(text.getPrimaryVerb(), "no primary verb for " + example[0]);
			assertEquals(example[1], text.getPrimaryVerb().getText(), example[0]);
			assertTrue(text.isImperative(), example[0] + " should be imperative");
		}
	}

	/**
	 * The roundtable names the tagger read as starting with a noun: retried with the first word
	 * tagged as a verb (it has a verb sense). The tokenizer splits "Force-stop" at the hyphen, so
	 * its verb is "Force".
	 */
	@Test
	void aCommandWhoseVerbTheTaggerReadAsANounIsRetriedAsACommand() {
		for (String[] example : new String[][] { { "Archive a room", "Archive" },
				{ "Respond to a CloudWatch alarm", "Respond" },
				{ "Recover a failing stream mid-session", "Recover" },
				{ "View the test room as a viewer", "View" },
				{ "Force-stop the active stream", "Force" },
				{ "Force-stop the stream during verification", "Force" } }) {
			NLPText text = process(example[0]);
			assertNotNull(text.getPrimaryVerb(), "no primary verb for " + example[0]);
			assertEquals(example[1], text.getPrimaryVerb().getText(), example[0]);
			assertTrue(text.isImperative(), example[0] + " should be imperative");
		}
	}

	/** A name whose first word has no verb sense is not retried and stays verbless. */
	@Test
	void aNounPhraseNameIsNotTurnedIntoACommand() {
		for (String example : new String[] { "Webinar dry run", "Manual verification pass" }) {
			NLPText text = process(example);
			assertNull(text.getPrimaryVerb(), example);
			assertFalse(text.isImperative(), example);
		}
	}

	@Test
	void aSentenceWithASubjectKeepsItsVerbAndIsNotImperative() {
		NLPText text = process("The host creates the room");
		assertEquals("creates", text.getPrimaryVerb().getText());
		assertFalse(text.isImperative());
	}

	@Test
	void aNameOrABracketedNoteHasNoPrimaryVerb() {
		for (String example : new String[] { "Webinar",
				"[Ingested verbatim from the source, criterion 1.]" }) {
			NLPText text = process(example);
			assertNull(text.getPrimaryVerb(), example);
			assertFalse(text.isImperative(), example);
		}
	}

	/**
	 * Role labelling runs on a command without failing and finds no agent. (It finds no other
	 * roles either: VerbNet's frames start with the subject, so "V NP" matches none. The step
	 * check needs only the missing agent.)
	 */
	@Test
	void roleLabellingACommandFindsNoAgent() {
		NLPText text = process("Create the room");
		getNlpProcessorFactory().getSemanticRoleLabeler().process(text);
		Map<SemanticRole, NLPText> roles = new SemanticRoleCollector(
				new SemanticRoleCollectorFunction(text.getPrimaryVerb())).process(text);
		assertNull(roles.get(SemanticRole.AGENT), "a command names no agent: " + roles);
	}
}
