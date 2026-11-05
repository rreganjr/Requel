/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.assistant.legacynlp;

import java.util.Map;

import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.SemanticRole;
import com.rreganjr.nlp.impl.srl.SemanticRoleCollector;
import com.rreganjr.nlp.impl.srl.SemanticRoleCollectorFunction;

/**
 * #349's reading of a step's or a scenario's name, shared by {@link StepStructureAssistant} (how
 * one step reads) and {@link StepSequenceAssistant} (#455, how a scenario's steps follow each
 * other).
 * <ul>
 * <li>{@link Kind#UNREADABLE}: no verb was found, or no subject for it.</li>
 * <li>{@link Kind#COMMAND}: an imperative ("Start the livestream"); {@code verb} is its verb.</li>
 * <li>{@link Kind#SUBJECT}: "&lt;subject&gt; does something"; {@code subject} is the agent as
 * written ("The host", "the system").</li>
 * </ul>
 */
record StepReading(Kind kind, String verb, String subject) {

	enum Kind {
		UNREADABLE, COMMAND, SUBJECT
	}

	private static final StepReading UNREADABLE = new StepReading(Kind.UNREADABLE, null, null);

	static StepReading unreadable() {
		return UNREADABLE;
	}

	static StepReading command(String verb) {
		return new StepReading(Kind.COMMAND, verb, null);
	}

	static StepReading subject(String subject) {
		return new StepReading(Kind.SUBJECT, null, subject);
	}

	/** Reads {@code name}, which must not be blank. */
	static StepReading read(NLPProcessorFactory nlpProcessorFactory, String name) {
		NLPText text = nlpProcessorFactory.processText(name);
		if (text.getPrimaryVerb() == null) {
			return UNREADABLE;
		}
		if (text.isImperative()) {
			return command(text.getPrimaryVerb().getText());
		}
		Map<SemanticRole, NLPText> roles = new SemanticRoleCollector(
				new SemanticRoleCollectorFunction(text.getPrimaryVerb())).process(text);
		NLPText agentText = roles.get(SemanticRole.AGENT);
		if (agentText == null || agentText.getText().isEmpty()) {
			return UNREADABLE;
		}
		return subject(agentText.getText());
	}
}
