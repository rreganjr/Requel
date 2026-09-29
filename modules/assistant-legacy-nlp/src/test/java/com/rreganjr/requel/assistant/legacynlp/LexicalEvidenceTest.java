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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.impl.NLPTextImpl;

/** Issue #268 / #269: quoted evidence must be text the entity actually contains. */
class LexicalEvidenceTest {

	private static List<NLPText> tokens(String... words) {
		return Arrays.stream(words).map(w -> (NLPText) new NLPTextImpl(w))
				.collect(Collectors.toList());
	}

	@Test
	void locateRestoresTheHyphenTheParserSplitOff() {
		assertThat(LexicalEvidence.locate("The Operator force-stops the stream.",
				tokens("force", "-", "stops", "the", "stream"))).isEqualTo("force-stops the stream");
	}

	@Test
	void locateMatchesParserQuoteAndBracketTokens() {
		assertThat(LexicalEvidence.locate("It archives “the room” — always.",
				tokens("``", "the", "room", "''"))).isEqualTo("“the room”");
		assertThat(LexicalEvidence.locate("the channel (IVS) is", tokens("-LRB-", "IVS", "-RRB-")))
				.isEqualTo("(IVS)");
	}

	@Test
	void locateKeepsTheAuthorsCase() {
		assertThat(LexicalEvidence.locate("Rotate the Stream Key now.", tokens("stream", "key")))
				.isEqualTo("Stream Key");
	}

	@Test
	void locateRejectsAFragmentThatStartsOrEndsMidWord() {
		assertThat(LexicalEvidence.locate("the permissions matrix", tokens("ermissions", "matrix")))
				.isNull();
		assertThat(LexicalEvidence.locate("a Zoom webinar panel", tokens("a", "Zoom", "webinar", "p")))
				.isNull();
	}

	@Test
	void occursRespectsWordBoundariesAcrossDashesAndNonAsciiPunctuation() {
		String text = "End the room — then archive it – or “force-stop” the stream.";
		assertThat(LexicalEvidence.occurs(text, "the room")).isTrue();
		assertThat(LexicalEvidence.occurs(text, "archive")).isTrue();
		assertThat(LexicalEvidence.occurs(text, "force-stop")).isTrue();
		assertThat(LexicalEvidence.occurs(text, "stream")).isTrue();
		assertThat(LexicalEvidence.occurs("the roomy lobby", "the room")).isFalse();
		assertThat(LexicalEvidence.occurs("the permissions matrix", "ermissions")).isFalse();
		assertThat(LexicalEvidence.occurs("the recording notice", "notic")).isFalse();
	}

	@Test
	void occursToleratesDifferentWhitespaceAndCase() {
		assertThat(LexicalEvidence.occurs("a  test\nroom", "Test room")).isTrue();
	}

	@Test
	void occursLetsASplitOffCliticThrough() {
		assertThat(LexicalEvidence.occurs("It doesn't stream.", "n't")).isTrue();
	}

	@Test
	void occursRejectsBlankInput() {
		assertThat(LexicalEvidence.occurs("text", " ")).isFalse();
		assertThat(LexicalEvidence.occurs(null, "text")).isFalse();
		assertThat(LexicalEvidence.occurs("text", null)).isFalse();
	}

	@Test
	void occursMatchesAPhraseThatStartsOrEndsWithPunctuation() {
		assertThat(LexicalEvidence.occurs("see (the room) now", "(the room)")).isTrue();
		assertThat(LexicalEvidence.occurs("see the room now", "(the room)")).isFalse();
	}

	@Test
	void locateMapsEveryParserEscapeBackToTheSourceCharacter() {
		assertThat(LexicalEvidence.locate("the list [draft] is", tokens("-LSB-", "draft", "-RSB-")))
				.isEqualTo("[draft]");
		assertThat(LexicalEvidence.locate("the map {key} is", tokens("-LCB-", "key", "-RCB-")))
				.isEqualTo("{key}");
		assertThat(LexicalEvidence.locate("it ends — now", tokens("ends", "--", "now")))
				.isEqualTo("ends — now");
		assertThat(LexicalEvidence.locate("it ends -- now", tokens("ends", "--", "now")))
				.isEqualTo("ends -- now");
		assertThat(LexicalEvidence.locate("it ends – now", tokens("ends", "--", "now")))
				.isEqualTo("ends – now");
	}

	@Test
	void locateReturnsNullForMissingInput() {
		assertThat(LexicalEvidence.locate(null, tokens("room"))).isNull();
		assertThat(LexicalEvidence.locate("the room", null)).isNull();
		assertThat(LexicalEvidence.locate("the room", List.of())).isNull();
		assertThat(LexicalEvidence.locate("the room", tokens("the", ""))).isNull();
		assertThat(LexicalEvidence.locate("the room", List.of(new NLPTextImpl((String) null))))
				.isNull();
	}
}
