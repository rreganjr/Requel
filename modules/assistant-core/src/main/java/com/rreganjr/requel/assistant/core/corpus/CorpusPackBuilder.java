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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.context.ContextPackSizeLimits;
import com.rreganjr.requel.assistant.core.context.RedactionPolicy;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Hint;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Member;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.ScoredPair;
import com.rreganjr.requel.assistant.core.corpus.CorpusMembers.CorpusSet;
import com.rreganjr.requel.assistant.core.corpus.CorpusPack.CandidateEntry;
import com.rreganjr.requel.assistant.core.corpus.CorpusPack.IndexEntry;

/**
 * Issue #266: builds the {@link CorpusPack} for a set within a character budget. Every name and
 * text goes through the project's {@link RedactionPolicy} first.
 *
 * <ul>
 * <li>The index (every member, text cut to {@code indexTextChars}, its links) must fit the
 * budget, or the build refuses with {@link CorpusTooLargeException}: analysing a truncated set and
 * reporting confident findings about the part it saw is worse than no analysis.</li>
 * <li>Candidates are added in rank order, each with both texts (up to the per-field maximum), while
 * they fit. Any left out make the pack {@code partial}.</li>
 * </ul>
 * Sizes are characters of content plus a fixed overhead per entry for the JSON around it.
 */
@Component
public class CorpusPackBuilder {

	/** Characters counted for the JSON around one entry (keys, quotes, punctuation). */
	static final int ENTRY_OVERHEAD = 60;

	/** The set's text, redacted, by member: what evidence is checked against. */
	public record Built(CorpusPack pack, Map<EntityRef, String> sentText) {
	}

	/** The index alone is over the budget: nothing is analysed. */
	public static class CorpusTooLargeException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private final int indexChars;
		private final int budget;

		public CorpusTooLargeException(String message, int indexChars, int budget) {
			super(message);
			this.indexChars = indexChars;
			this.budget = budget;
		}

		public int indexChars() {
			return indexChars;
		}

		public int budget() {
			return budget;
		}
	}

	private final RedactionPolicy redactionPolicy;
	private final ContextPackSizeLimits limits;

	@Autowired
	public CorpusPackBuilder(RedactionPolicy redactionPolicy, ContextPackSizeLimits limits) {
		this.redactionPolicy = Objects.requireNonNull(redactionPolicy, "redactionPolicy");
		this.limits = Objects.requireNonNull(limits, "limits");
	}

	/**
	 * @param rootLabel how the refusal names the set's root, e.g. {@code Project 12}
	 * @param candidates the finder's candidates, highest score first
	 * @param maxChars the input budget in characters ({@code requel.ai.corpus.max-input-chars})
	 * @param indexTextChars how much of each member's text the index carries
	 * @throws CorpusTooLargeException when the index alone is over {@code maxChars}
	 */
	public Built build(CorpusSet set, String setKind, EntityRef root, String rootLabel,
			Long projectId, List<ScoredPair> candidates, int maxChars, int indexTextChars) {
		RedactionPolicy policy = redactionPolicy.forProject(projectId);
		List<String> redacted = new ArrayList<>();
		Map<EntityRef, String> names = new HashMap<>();
		Map<EntityRef, String> texts = new HashMap<>();
		Map<EntityRef, String> sent = new HashMap<>();
		for (Member member : set.members()) {
			String path = member.ref().entityType() + "[" + member.ref().entityId() + "]";
			String name = redact(policy, path + ".name", member.name(), redacted);
			String text = redact(policy, path + ".text", member.text(), redacted);
			names.put(member.ref(), name);
			texts.put(member.ref(), text);
			sent.put(member.ref(), (name == null ? "" : name) + "\n" + (text == null ? "" : text));
		}

		List<IndexEntry> index = new ArrayList<>();
		int indexChars = 0;
		for (Member member : set.members()) {
			List<String> linked = new ArrayList<>();
			for (Member other : set.members()) {
				if (!other.ref().equals(member.ref()) && set.linked(member.ref(), other.ref())) {
					linked.add(ref(other.ref()));
				}
			}
			IndexEntry entry = new IndexEntry(ref(member.ref()), names.get(member.ref()),
					cut(texts.get(member.ref()), indexTextChars), List.copyOf(linked));
			index.add(entry);
			indexChars += size(entry.ref(), entry.name(), entry.text()) + 16 * linked.size();
		}
		if (indexChars > maxChars) {
			throw new CorpusTooLargeException("The corpus index for " + rootLabel + " is "
					+ String.format(java.util.Locale.ROOT, "%,d", indexChars)
					+ " characters; the limit is "
					+ String.format(java.util.Locale.ROOT, "%,d", maxChars)
					+ " (requel.ai.corpus.max-input-chars). Analyse a goal or use case instead, or"
					+ " raise the limit. Nothing was analysed.", indexChars, maxChars);
		}

		int fieldMax = limits.getMaxTextCharsPerField();
		int total = indexChars;
		List<CandidateEntry> sentCandidates = new ArrayList<>();
		for (ScoredPair pair : candidates) {
			List<String> hints = new ArrayList<>();
			for (Hint hint : pair.hints()) {
				hints.add(hint.type() + " " + hint.describe());
			}
			CandidateEntry entry = new CandidateEntry(ref(pair.a()), ref(pair.b()),
					pair.kind() == null ? "OVERLAP" : pair.kind().name(), pair.score(),
					List.copyOf(hints), cut(texts.get(pair.a()), fieldMax),
					cut(texts.get(pair.b()), fieldMax));
			int entrySize = size(entry.a(), entry.b(), entry.textA(), entry.textB())
					+ 40 * hints.size();
			if (total + entrySize > maxChars) {
				break;
			}
			total += entrySize;
			sentCandidates.add(entry);
		}
		CorpusPack.Notes notes = new CorpusPack.Notes(set.members().size(), candidates.size(),
				sentCandidates.size(), sentCandidates.size() < candidates.size(), indexChars, total,
				List.copyOf(redacted));
		return new Built(new CorpusPack(setKind, ref(root), List.copyOf(index),
				List.copyOf(sentCandidates), notes), Map.copyOf(sent));
	}

	/** {@code Type:id}. */
	public static String ref(EntityRef ref) {
		return ref.entityType() + ":" + ref.entityId();
	}

	private static String redact(RedactionPolicy policy, String path, String value,
			List<String> notes) {
		return value == null ? null : policy.redact(path, value, notes);
	}

	private static String cut(String text, int max) {
		if (text == null || max <= 0 || text.length() <= max) {
			return text;
		}
		return text.substring(0, max).stripTrailing() + "...";
	}

	private static int size(String... parts) {
		int size = ENTRY_OVERHEAD;
		for (String part : parts) {
			size += part == null ? 0 : part.length();
		}
		return size;
	}
}
