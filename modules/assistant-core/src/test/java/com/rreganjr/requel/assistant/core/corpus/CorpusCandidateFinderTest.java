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
package com.rreganjr.requel.assistant.core.corpus;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.HintType;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Kind;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Member;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.ScoredPair;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Settings;

/** Issue #266: the stage-1 candidate finder, with a fake dictionary. */
class CorpusCandidateFinderTest {

	private static final WordRelations DICTIONARY = new WordRelations() {
		private final Map<String, Set<String>> synonyms = Map.of(
				"buy", Set.of("purchase", "get"),
				"purchase", Set.of("buy"),
				"catalogue", Set.of("catalog", "list"));
		private final Map<String, Set<String>> antonyms = Map.of(
				"increase", Set.of("decrease"),
				"decrease", Set.of("increase"),
				"private", Set.of("public"),
				"public", Set.of("private"));

		@Override
		public Set<String> synonyms(String word) {
			return synonyms.getOrDefault(word, Set.of());
		}

		@Override
		public Set<String> antonyms(String word) {
			return antonyms.getOrDefault(word, Set.of());
		}
	};

	private static Member goal(long id, String name, String text) {
		return new Member(EntityRef.of("Goal", id), name, text);
	}

	private static ScoredPair pair(List<ScoredPair> pairs, long a, long b) {
		return pairs.stream()
				.filter(p -> p.a().entityId() == a && p.b().entityId() == b
						|| p.a().entityId() == b && p.b().entityId() == a)
				.findFirst().orElse(null);
	}

	@Test
	void synonymsInTheSetRaiseTheScore() {
		List<Member> members = List.of(
				goal(1, "Members buy tools online", null),
				goal(2, "Members purchase tools online", null),
				goal(3, "Volunteers repair bicycles weekly", null));
		List<ScoredPair> with = new CorpusCandidateFinder(DICTIONARY, Settings.DEFAULTS)
				.scoreAll(members, Map.of());
		ScoredPair p = pair(with, 1, 2);
		assertThat(p.score()).isGreaterThan(p.plainScore());
		assertThat(pair(with, 1, 3)).isNull();
	}

	@Test
	void withoutADictionaryTheScoreIsPlain() {
		List<Member> members = List.of(
				goal(1, "Members buy tools online", null),
				goal(2, "Members purchase tools online", null));
		ScoredPair p = new CorpusCandidateFinder(WordRelations.NONE, Settings.DEFAULTS)
				.scoreAll(members, Map.of()).get(0);
		assertThat(p.score()).isEqualTo(p.plainScore());
	}

	@Test
	void differentNumbersForTheSameUnitAreAConflict() {
		List<Member> members = List.of(
				goal(1, "Loans last 14 days", "A member keeps a tool for 14 days."),
				goal(2, "Loan length", "A loan lasts 21 days from checkout."),
				goal(3, "Volunteers repair bicycles weekly", "Two volunteers meet each week."));
		List<ScoredPair> pairs = new CorpusCandidateFinder(DICTIONARY, Settings.DEFAULTS)
				.find(members, Map.of());
		ScoredPair p = pair(pairs, 1, 2);
		assertThat(p.kind()).isEqualTo(Kind.CONFLICT);
		assertThat(p.hints()).anySatisfy(h -> {
			assertThat(h.type()).isEqualTo(HintType.NUMBER);
			assertThat(h.describe()).contains("14 days").contains("21 days");
		});
	}

	@Test
	void theSameNumberIsNoConflict() {
		List<Member> members = List.of(
				goal(1, "Loans last 14 days", "A member keeps a tool for 14 days."),
				goal(2, "Loan length", "A loan lasts 14 days from checkout."));
		ScoredPair p = new CorpusCandidateFinder(DICTIONARY, Settings.DEFAULTS)
				.scoreAll(members, Map.of()).get(0);
		assertThat(p.hints()).noneMatch(h -> h.type() == HintType.NUMBER);
	}

	@Test
	void antonymsOnASimilarPairAreAConflict() {
		List<Member> members = List.of(
				goal(1, "Member phone numbers stay private", null),
				goal(2, "Member phone numbers are public on the loan list", null));
		ScoredPair p = new CorpusCandidateFinder(DICTIONARY, Settings.DEFAULTS)
				.find(members, Map.of()).get(0);
		assertThat(p.kind()).isEqualTo(Kind.CONFLICT);
		assertThat(p.hints()).anySatisfy(h -> {
			assertThat(h.type()).isEqualTo(HintType.ANTONYM);
			assertThat(h.inA()).isEqualTo("private");
			assertThat(h.inB()).isEqualTo("public");
		});
	}

	@Test
	void oppositePolarityOnASharedVerbIsAConflict() {
		List<Member> members = List.of(
				goal(1, "Reminders", "The system must send overdue reminders by text message."),
				goal(2, "Privacy", "The system must not send member data by text message."));
		ScoredPair p = new CorpusCandidateFinder(DICTIONARY, Settings.DEFAULTS)
				.find(members, Map.of()).get(0);
		assertThat(p.hints()).anySatisfy(h -> {
			assertThat(h.type()).isEqualTo(HintType.NEGATION);
			assertThat(h.inA()).isEqualTo("must send");
			assertThat(h.inB()).isEqualTo("must not send");
		});
	}

	@Test
	void cannotCountsAsNegation() {
		List<Member> members = List.of(
				goal(1, "Suspended members", "Suspended members cannot borrow tools."),
				goal(2, "Members borrow", "Suspended members can borrow tools on weekends."));
		ScoredPair p = new CorpusCandidateFinder(DICTIONARY, Settings.DEFAULTS)
				.find(members, Map.of()).get(0);
		assertThat(p.hints()).anyMatch(h -> h.type() == HintType.NEGATION);
	}

	@Test
	void aSimilarPairWithoutHintsIsAnOverlap() {
		List<Member> members = List.of(
				goal(1, "Members search the catalogue by name", null),
				goal(2, "Members search the catalogue quickly", null),
				goal(3, "Volunteers repair bicycles weekly", null));
		List<ScoredPair> pairs = new CorpusCandidateFinder(DICTIONARY, Settings.DEFAULTS)
				.find(members, Map.of());
		assertThat(pairs).hasSize(1);
		assertThat(pairs.get(0).kind()).isEqualTo(Kind.OVERLAP);
		assertThat(pairs.get(0).hints()).isEmpty();
	}

	@Test
	void belowBothThresholdsIsNoCandidate() {
		List<Member> members = List.of(
				goal(1, "Members search the catalogue by name", "Search covers tool names."),
				goal(2, "Volunteers plan repairs", "Repairs are planned by name of volunteer."));
		Settings strict = new Settings(0.9, 0.9, 0.5);
		assertThat(new CorpusCandidateFinder(DICTIONARY, strict).find(members, Map.of()))
				.isEmpty();
	}

	@Test
	void glossaryPhrasesCountAsOneToken() {
		List<Member> members = List.of(
				goal(1, "Schedule a web conference", null),
				goal(2, "Schedule a webinar", null));
		Map<String, String> glossary = Map.of("web conference", "1", "webinar", "1");
		ScoredPair with = new CorpusCandidateFinder(WordRelations.NONE, Settings.DEFAULTS)
				.scoreAll(members, glossary).get(0);
		assertThat(with.score()).isEqualTo(1.0);
	}

	@Test
	void theOrderIsDeterministic() {
		List<Member> members = List.of(
				goal(3, "Members search the catalogue", null),
				goal(1, "Members search the catalogue by name", null),
				goal(2, "Members search tools", null));
		CorpusCandidateFinder finder = new CorpusCandidateFinder(DICTIONARY, Settings.DEFAULTS);
		assertThat(finder.scoreAll(members, Map.of()))
				.isEqualTo(new CorpusCandidateFinder(DICTIONARY, Settings.DEFAULTS)
						.scoreAll(List.of(members.get(2), members.get(0), members.get(1)),
								Map.of()));
	}

	@Test
	void aLinkedPairIsNoOverlapButCanStillConflict() {
		List<Member> members = List.of(
				goal(1, "Members search the catalogue by name", null),
				goal(2, "Members search the catalogue quickly", null),
				goal(3, "Loans last 14 days", "A member keeps a tool for 14 days."),
				goal(4, "Loan length", "A loan lasts 21 days from checkout."));
		CorpusCandidateFinder.Links links = (a, b) -> true;
		List<ScoredPair> pairs = new CorpusCandidateFinder(DICTIONARY, Settings.DEFAULTS)
				.find(members, Map.of(), links);
		assertThat(pair(pairs, 1, 2)).isNull();
		assertThat(pair(pairs, 3, 4).kind()).isEqualTo(Kind.CONFLICT);
		assertThat(pair(pairs, 3, 4).linked()).isTrue();
	}

	@Test
	void aHintIsReportedOnceWithTheNumberAndItsUnit() {
		List<Member> members = List.of(
				goal(1, "Two-week loans", "Every tool is due back 14 days after checkout."),
				goal(2, "Three-week loans", "Every tool is due back 21 days after checkout."));
		ScoredPair p = new CorpusCandidateFinder(DICTIONARY, Settings.DEFAULTS)
				.find(members, Map.of()).get(0);
		assertThat(p.hints()).hasSize(1);
		assertThat(p.hints().get(0).describe()).isEqualTo("'14 days' / '21 days'");
	}

	@Test
	void onlyTypesThatCanRepeatEachOtherOverlap() {
		List<Member> members = List.of(
				new Member(EntityRef.of("Actor", 1L), "Viewer", "Someone who watches the session."),
				new Member(EntityRef.of("Step", 2L), "Viewer watches the session", null));
		List<ScoredPair> pairs = new CorpusCandidateFinder(DICTIONARY, Settings.DEFAULTS)
				.scoreAll(members, Map.of());
		assertThat(pairs.get(0).score()).isGreaterThan(Settings.DEFAULTS.overlapThreshold());
		assertThat(pairs.get(0).kind()).isNull();
		assertThat(CorpusCandidateFinder.comparable("Goal", "UseCase")).isTrue();
		assertThat(CorpusCandidateFinder.comparable("GlossaryTerm", "Actor")).isTrue();
		assertThat(CorpusCandidateFinder.comparable("Step", "Step")).isFalse();
		assertThat(CorpusCandidateFinder.comparable("Actor", "Story")).isFalse();
	}

	@Test
	void antonymsAboutDifferentThingsAreNoHint() {
		// roundtable tuning: "live" and "recording" named different things, not a conflict
		List<Member> members = List.of(
				goal(1, "Member phone numbers stay private", "Reminders go out by email."),
				goal(2, "Member phone numbers on file",
						"Tools: the catalogue is public to everyone visiting the website."));
		ScoredPair p = new CorpusCandidateFinder(DICTIONARY, new Settings(0.35, 0.05, 0.5))
				.scoreAll(members, Map.of()).get(0);
		assertThat(p.hints()).noneMatch(h -> h.type() == HintType.ANTONYM);
	}
}
