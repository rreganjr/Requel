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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.context.ContextPackSizeLimits;
import com.rreganjr.requel.assistant.core.context.RedactionPolicy;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Member;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.ScoredPair;
import com.rreganjr.requel.assistant.core.corpus.CorpusMembers.CorpusSet;
import com.rreganjr.requel.assistant.core.corpus.CorpusPackBuilder.Built;
import com.rreganjr.requel.assistant.core.corpus.CorpusPackBuilder.CorpusTooLargeException;

/** Issue #266: the corpus pack: index, candidates, budget, redaction. */
class CorpusPackBuilderTest {

	private static final EntityRef G1 = EntityRef.of("Goal", 1L);
	private static final EntityRef G2 = EntityRef.of("Goal", 2L);
	private static final EntityRef UC = EntityRef.of("UseCase", 3L);
	private static final EntityRef ROOT = EntityRef.of("Project", 9L);

	/** Masks "secret" and notes it. */
	private static final RedactionPolicy MASK = (path, value, notes) -> {
		if (value.contains("secret")) {
			notes.add(path + " redacted");
			return value.replace("secret", "[masked]");
		}
		return value;
	};

	private final CorpusPackBuilder builder = new CorpusPackBuilder(MASK, new ContextPackSizeLimits());

	private final CorpusSet set = new CorpusSet(List.of(
			new Member(G1, "Two-week loans", "Every tool is due back 14 days after checkout. " + "x".repeat(400)),
			new Member(G2, "Three-week loans", "Every tool is due back 21 days after checkout."),
			new Member(UC, "Renew a loan", "The secret renewal flow.")),
			Set.of(Set.of(G1, UC)), Map.of(), Map.of());

	private final List<ScoredPair> candidates = List.of(
			new ScoredPair(G1, G2, 0.5, 0.5, List.of(new CorpusCandidateFinder.Hint(
					CorpusCandidateFinder.HintType.NUMBER, "14 days", "21 days")),
					CorpusCandidateFinder.Kind.CONFLICT, false),
			new ScoredPair(G2, UC, 0.3, 0.3, List.of(), CorpusCandidateFinder.Kind.OVERLAP, false));

	@Test
	void theIndexCarriesEveryMemberCutShortWithItsLinks() {
		Built built = builder.build(set, "PROJECT", ROOT, "Project 9", 9L, candidates, 100_000, 40);
		CorpusPack pack = built.pack();
		assertThat(pack.set()).isEqualTo("PROJECT");
		assertThat(pack.root()).isEqualTo("Project:9");
		assertThat(pack.index()).extracting(CorpusPack.IndexEntry::ref)
				.containsExactly("Goal:1", "Goal:2", "UseCase:3");
		assertThat(pack.index().get(0).text()).hasSizeLessThanOrEqualTo(43).endsWith("...");
		assertThat(pack.index().get(0).linkedTo()).containsExactly("UseCase:3");
		assertThat(pack.candidates()).hasSize(2);
		assertThat(pack.candidates().get(0).hints()).containsExactly("NUMBER '14 days' / '21 days'");
		assertThat(pack.candidates().get(0).textA()).contains("14 days after checkout");
		assertThat(pack.notes().partial()).isFalse();
	}

	@Test
	void namesAndTextsAreRedactedFirst() {
		Built built = builder.build(set, "PROJECT", ROOT, "Project 9", 9L, candidates, 100_000, 240);
		assertThat(built.pack().index().get(2).text()).isEqualTo("The [masked] renewal flow.");
		assertThat(built.sentText().get(UC)).contains("[masked]").doesNotContain("secret");
		assertThat(built.pack().notes().redacted()).containsExactly("UseCase[3].text redacted");
	}

	@Test
	void candidatesThatDoNotFitMakeThePackPartial() {
		Built all = builder.build(set, "PROJECT", ROOT, "Project 9", 9L, candidates, 100_000, 40);
		int indexOnly = all.pack().notes().indexChars();
		Built built = builder.build(set, "PROJECT", ROOT, "Project 9", 9L, candidates,
				indexOnly + 700, 40);
		assertThat(built.pack().candidates()).hasSize(1);
		assertThat(built.pack().notes().partial()).isTrue();
		assertThat(built.pack().notes().candidatesFound()).isEqualTo(2);
		assertThat(built.pack().notes().candidatesSent()).isEqualTo(1);
	}

	@Test
	void anIndexOverTheBudgetIsRefusedWithTheOverflowNamed() {
		assertThatThrownBy(() -> builder.build(set, "PROJECT", ROOT, "Project 9", 9L, candidates,
				100, 40))
				.isInstanceOf(CorpusTooLargeException.class)
				.hasMessageContaining("The corpus index for Project 9 is")
				.hasMessageContaining("the limit is 100 (requel.ai.corpus.max-input-chars)")
				.hasMessageContaining("Nothing was analysed.");
	}
}
