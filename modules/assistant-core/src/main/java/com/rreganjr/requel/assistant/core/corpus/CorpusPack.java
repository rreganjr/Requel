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

import java.util.List;

/**
 * Issue #266: what a corpus analysis sends the model. An {@link IndexEntry} for every member of
 * the set, its text cut short, with the members it is structurally linked to; then the finder's
 * candidate pairs in rank order with both texts in full and the finder's hints.
 *
 * @param set the set kind (PROJECT, GOAL, USE_CASE)
 * @param root the set's root, {@code Type:id}
 */
public record CorpusPack(String set, String root, List<IndexEntry> index,
		List<CandidateEntry> candidates, Notes notes) {

	/**
	 * One member of the set.
	 *
	 * @param ref {@code Type:id}, the form a finding names its participants in
	 * @param text the member's text, cut to the index length ({@code "..."} marks a cut)
	 * @param linkedTo the members it is structurally linked to (its scenario, its actor, ...)
	 */
	public record IndexEntry(String ref, String name, String text, List<String> linkedTo) {
	}

	/** A pair the finder thinks is worth judging. */
	public record CandidateEntry(String a, String b, String kind, double score, List<String> hints,
			String textA, String textB) {
	}

	/**
	 * What was and wasn't sent. {@code partial} is true when some candidates didn't fit the
	 * input budget; the run then says so and is never reported as complete.
	 */
	public record Notes(int members, int candidatesFound, int candidatesSent, boolean partial,
			int indexChars, int totalChars, List<String> redacted) {
	}
}
