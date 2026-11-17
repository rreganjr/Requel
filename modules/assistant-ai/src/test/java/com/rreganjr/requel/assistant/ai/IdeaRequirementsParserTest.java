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
package com.rreganjr.requel.assistant.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Draft;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Kind;
import com.rreganjr.requel.assistant.ai.IdeaToRequirementsService.Idea;

class IdeaRequirementsParserTest {

	private static final Idea IDEA = new Idea("Let gym members book a class from their phone.",
			List.of("Existing goal"), List.of("Admin"), List.of());

	private static AiFindingDraft finding(String type, String name, String text,
			String... participants) {
		return new AiFindingDraft(type, null, 0.8, List.of(), text, null, List.of(),
				Map.of(ReviewResultMapper.SUGGESTED_ENTITY_NAME, name,
						ReviewResultMapper.PARTICIPANTS, List.of(participants)));
	}

	private static IdeaRequirementsDrafts parse(AiFindingDraft... findings) {
		return IdeaRequirementsParser.parse("summary", List.of(findings), List.of(), IDEA, 25);
	}

	private static long dropped(IdeaRequirementsDrafts drafts) {
		return drafts.problems().stream().filter(p -> p.dropped()).count();
	}

	@Test
	void resolvesReferencesAndPutsThePrimaryActorFirst() {
		IdeaRequirementsDrafts drafts = parse(
				finding("ACTOR", "Gym member", "A person who attends classes."),
				finding("GOAL", "Book remotely", "Members reserve a place without calling."),
				finding("USE_CASE", "Receive reminder", "A member is reminded before class.",
						"GOAL:Book remotely", "ACTOR:gym members", "GOAL:Existing goal"));

		assertEquals(3, drafts.drafts().size());
		Draft useCase = drafts.ofKind(Kind.USE_CASE).get(0);
		assertEquals(3, useCase.references().size());
		assertEquals(Kind.ACTOR, useCase.references().get(0).kind());
		assertEquals("Gym member", useCase.references().get(0).name());
		assertFalse(useCase.references().get(0).existing());
		assertTrue(useCase.references().get(2).existing());
		assertTrue(drafts.problems().isEmpty());
	}

	@Test
	void dropsUnknownKindsRepeatsAndNamesTheProjectHas() {
		IdeaRequirementsDrafts drafts = parse(
				finding("ACTOR", "Admin", "Already there."),
				finding("SCREEN", "Login screen", "Not an entity kind."),
				finding("GOAL", "Book remotely", "First."),
				finding("GOAL", "book remotely", "Repeat."),
				finding("GOAL", "No text", "  "));

		assertEquals(1, drafts.drafts().size());
		assertEquals("Book remotely", drafts.drafts().get(0).name());
		assertEquals(4, dropped(drafts));
	}

	@Test
	void dropsAStoryWithoutAnActorAndLeavesOutReferencesToIt() {
		IdeaRequirementsDrafts drafts = parse(
				finding("ACTOR", "Member", "A member."),
				finding("GOAL", "Book", "Book a class."),
				finding("STORY", "Fully booked", "A class is full.", "GOAL:Book"),
				finding("USE_CASE", "Join waitlist", "A member joins a waitlist.",
						"ACTOR:Member", "STORY:Fully booked"));

		assertEquals(3, drafts.drafts().size());
		assertTrue(drafts.ofKind(Kind.STORY).isEmpty());
		assertEquals(1, drafts.ofKind(Kind.USE_CASE).get(0).references().size());
		assertEquals(1, dropped(drafts));
	}

	@Test
	void keepsADraftWhoseReferenceCannotBeResolved() {
		IdeaRequirementsDrafts drafts = parse(
				finding("ACTOR", "Member", "A member."),
				finding("USE_CASE", "Join waitlist", "A member joins a waitlist.",
						"ACTOR:Member", "GOAL:Nowhere", "nonsense", "GLOSSARY_TERM:Member"));

		assertEquals(2, drafts.drafts().size());
		assertEquals(1, drafts.ofKind(Kind.USE_CASE).get(0).references().size());
		assertEquals(3, drafts.problems().size());
		assertEquals(0, dropped(drafts));
	}

	@Test
	void leavesOutAGoalReferenceThatWouldMakeACycle() {
		IdeaRequirementsDrafts drafts = parse(
				finding("GOAL", "First", "One.", "GOAL:Second"),
				finding("GOAL", "Second", "Two.", "GOAL:First"));

		assertEquals(2, drafts.drafts().size());
		assertEquals(1, drafts.drafts().get(0).references().size());
		assertTrue(drafts.drafts().get(1).references().isEmpty());
	}

	@Test
	void capsTheNumberOfProposals() {
		IdeaRequirementsDrafts drafts = IdeaRequirementsParser.parse("summary",
				List.of(finding("ACTOR", "One", "1."), finding("ACTOR", "Two", "2.")), List.of(),
				IDEA, 1);

		assertEquals(1, drafts.drafts().size());
		assertEquals(1, dropped(drafts));
	}
}
