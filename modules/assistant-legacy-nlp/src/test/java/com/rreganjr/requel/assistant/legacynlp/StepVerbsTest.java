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

import org.junit.jupiter.api.Test;

/** Issue #459: the "-s" form of a step's verb and the rewrite that names the actor. */
class StepVerbsTest {

	@Test
	void theThirdPersonFormFollowsTheSpellingRules() {
		assertThat(StepVerbs.thirdPerson("Download")).isEqualTo("downloads");
		assertThat(StepVerbs.thirdPerson("Issue")).isEqualTo("issues");
		assertThat(StepVerbs.thirdPerson("Promote")).isEqualTo("promotes");
		assertThat(StepVerbs.thirdPerson("push")).isEqualTo("pushes");
		assertThat(StepVerbs.thirdPerson("fix")).isEqualTo("fixes");
		assertThat(StepVerbs.thirdPerson("buzz")).isEqualTo("buzzes");
		assertThat(StepVerbs.thirdPerson("Watch")).isEqualTo("watches");
		assertThat(StepVerbs.thirdPerson("pass")).isEqualTo("passes");
		assertThat(StepVerbs.thirdPerson("Go")).isEqualTo("goes");
		assertThat(StepVerbs.thirdPerson("do")).isEqualTo("does");
		assertThat(StepVerbs.thirdPerson("Carry")).isEqualTo("carries");
		assertThat(StepVerbs.thirdPerson("play")).isEqualTo("plays");
		assertThat(StepVerbs.thirdPerson("Be")).isEqualTo("is");
		assertThat(StepVerbs.thirdPerson("have")).isEqualTo("has");
	}

	@Test
	void aHyphenatedVerbChangesItsLastPart() {
		assertThat(StepVerbs.thirdPerson("Force-stop")).isEqualTo("force-stops");
		assertThat(StepVerbs.thirdPerson("re-issue")).isEqualTo("re-issues");
	}

	@Test
	void anAcronymKeepsItsCase() {
		assertThat(StepVerbs.thirdPerson("SSH")).isEqualTo("SSHes");
	}

	@Test
	void theRewriteNamesTheSubjectAndKeepsTheRestAsWritten() {
		assertThat(StepVerbs.rewrite("Download the resulting MP4", "Download", "Host"))
				.isEqualTo("Host downloads the resulting MP4");
		assertThat(StepVerbs.rewrite("Download the resulting MP4", "download", "the system"))
				.isEqualTo("The system downloads the resulting MP4");
		assertThat(StepVerbs.rewrite("Issue a new takeover key", "Issue", "Host"))
				.isEqualTo("Host issues a new takeover key");
		assertThat(StepVerbs.rewrite("Hand the stream key to Zoom", "Hand", "IT Admin"))
				.isEqualTo("IT Admin hands the stream key to Zoom");
	}

	@Test
	void theVerbIsWidenedToTheHyphenatedWordAroundIt() {
		assertThat(StepVerbs.rewrite("Force-stop the active stream", "stop", "Host"))
				.isEqualTo("Host force-stops the active stream");
		assertThat(StepVerbs.rewrite("Force-stop the active stream", "Force-stop", "Host"))
				.isEqualTo("Host force-stops the active stream");
	}

	@Test
	void wordsBeforeTheVerbAreKept() {
		assertThat(StepVerbs.rewrite("Then download the file", "download", "Host"))
				.isEqualTo("Then Host downloads the file");
		assertThat(StepVerbs.rewrite("Then download the file", "download", "the system"))
				.isEqualTo("Then the system downloads the file");
	}

	@Test
	void aVerbThatIsNotAWholeWordInTheNameGivesNoRewrite() {
		assertThat(StepVerbs.rewrite("Run the nonstop stream", "stop", "Host")).isNull();
		assertThat(StepVerbs.rewrite("Start the stream", "end", "Host")).isNull();
		assertThat(StepVerbs.rewrite(null, "end", "Host")).isNull();
	}
}
