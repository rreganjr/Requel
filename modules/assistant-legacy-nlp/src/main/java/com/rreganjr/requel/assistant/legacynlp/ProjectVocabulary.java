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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomain;

/**
 * Issue #268: the words a project has defined for itself — its glossary term names and actor
 * names, whole and word by word, lower-cased. A project's own vocabulary is correctly spelled by
 * definition ("Passthrough remux" makes both "passthrough" and "remux" words of the project), and
 * the glossary check uses the whole names to recognise a phrase the project already defines.
 */
public final class ProjectVocabulary {

	private static final ProjectVocabulary EMPTY = new ProjectVocabulary(Set.of(), Set.of(),
			Set.of());

	private final Set<String> termNames;
	private final Set<String> actorNames;
	private final Set<String> words;

	private ProjectVocabulary(Set<String> termNames, Set<String> actorNames, Set<String> words) {
		this.termNames = termNames;
		this.actorNames = actorNames;
		this.words = words;
	}

	/** The vocabulary of {@code projectOrDomain}; empty when it is null. Reads its terms and actors. */
	public static ProjectVocabulary of(ProjectOrDomain projectOrDomain) {
		if (projectOrDomain == null) {
			return EMPTY;
		}
		List<String> terms = projectOrDomain.getGlossaryTerms() == null ? List.of()
				: projectOrDomain.getGlossaryTerms().stream().map(GlossaryTerm::getName).toList();
		List<String> actors = projectOrDomain.getActors() == null ? List.of()
				: projectOrDomain.getActors().stream().map(Actor::getName).toList();
		return of(terms, actors);
	}

	public static ProjectVocabulary of(Collection<String> termNames, Collection<String> actorNames) {
		Set<String> terms = new HashSet<>();
		Set<String> actors = new HashSet<>();
		Set<String> words = new HashSet<>();
		add(termNames, terms, words);
		add(actorNames, actors, words);
		return new ProjectVocabulary(Set.copyOf(terms), Set.copyOf(actors), Set.copyOf(words));
	}

	private static void add(Collection<String> names, Set<String> into, Set<String> words) {
		if (names == null) {
			return;
		}
		for (String name : names) {
			if (name == null || name.isBlank()) {
				continue;
			}
			String lower = name.trim().toLowerCase(Locale.ROOT);
			into.add(lower);
			for (String word : lower.split("[\\s/]+")) {
				if (word.isEmpty()) {
					continue;
				}
				words.add(word);
				if (word.indexOf('-') > 0) {
					for (String part : word.split("-")) {
						if (!part.isEmpty()) {
							words.add(part);
						}
					}
				}
			}
		}
	}

	/** Whether {@code token} is a term or actor name, or one word of one (case-insensitive). */
	public boolean contains(String token) {
		if (token == null) {
			return false;
		}
		String lower = token.toLowerCase(Locale.ROOT);
		return words.contains(lower) || termNames.contains(lower) || actorNames.contains(lower);
	}

	/** Whether {@code phrase} is a glossary term name (case-insensitive). */
	public boolean isTermName(String phrase) {
		return phrase != null && termNames.contains(phrase.trim().toLowerCase(Locale.ROOT));
	}

	/** Whether {@code phrase} is an actor name (case-insensitive). */
	public boolean isActorName(String phrase) {
		return phrase != null && actorNames.contains(phrase.trim().toLowerCase(Locale.ROOT));
	}
}
