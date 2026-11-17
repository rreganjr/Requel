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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.rreganjr.requel.assistant.api.AssistantMessage;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Draft;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Kind;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Problem;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Reference;
import com.rreganjr.requel.assistant.ai.IdeaToRequirementsService.Idea;

/**
 * Parses and validates the reply to an idea-to-requirements call into
 * {@link IdeaRequirementsDrafts}. The reply is untrusted input: nothing is trusted to be
 * well-formed, and nothing is thrown for a bad proposal; it is dropped, or the bad part is left
 * out, and the reason is recorded as a {@link Problem}.
 *
 * <p>
 * Each finding is checked in turn: its kind must be one of {@link Kind}, it needs a name and a
 * text, and it must not repeat an earlier proposal or a name the project already has (compared
 * ignoring case and a trailing "s"). Proposals past {@code maxProposals} are dropped. Then the
 * {@code participants} of each survivor are resolved to the other proposals or to the project's
 * existing goals, actors and glossary terms:
 * <ul>
 * <li>a goal may refer to goals, a story to actors and goals, a use case to actors, goals and
 * stories; an actor or glossary term to nothing;</li>
 * <li>a malformed, unknown, wrong-kind, self-referring or dangling reference is left out, as is
 * a goal reference that would make a goal part of itself;</li>
 * <li>a story or use case with no actor is dropped, and its first actor is moved to the front;</li>
 * <li>evidence not found in the idea text flags the draft, it does not drop it.</li>
 * </ul>
 */
public final class IdeaRequirementsParser {

	private static final int MAX_NAME_LENGTH = 255;

	private static final int MAX_EVIDENCE = 20;

	private static final String ASSUMPTION_PREFIX = "assumption:";

	/** What each kind may refer to. */
	private static final Map<Kind, Set<Kind>> ALLOWED_REFERENCES = new EnumMap<>(Kind.class);

	static {
		ALLOWED_REFERENCES.put(Kind.GOAL, EnumSet.of(Kind.GOAL));
		ALLOWED_REFERENCES.put(Kind.ACTOR, EnumSet.noneOf(Kind.class));
		ALLOWED_REFERENCES.put(Kind.STORY, EnumSet.of(Kind.ACTOR, Kind.GOAL));
		ALLOWED_REFERENCES.put(Kind.USE_CASE, EnumSet.of(Kind.ACTOR, Kind.GOAL, Kind.STORY));
		ALLOWED_REFERENCES.put(Kind.GLOSSARY_TERM, EnumSet.noneOf(Kind.class));
	}

	/** A proposal's identity: its kind and its normalized name. */
	private record Key(Kind kind, String name) {
	}

	/** A proposal that passed the checks of its own finding. */
	private record Candidate(String name, String text, String note, boolean assumption,
			List<String> evidence, boolean evidenceUnverified, Double confidence,
			List<String> participants) {
	}

	private IdeaRequirementsParser() {
	}

	/**
	 * @param idea the idea that was sent: its text is what evidence is checked against and its
	 *        name lists are the existing entities
	 * @param maxProposals the most drafts to keep
	 */
	public static IdeaRequirementsDrafts parse(AiAnalysisResponse response, Idea idea,
			int maxProposals) {
		return parse(response.summary(), response.findings(), response.messages(), idea,
				maxProposals);
	}

	public static IdeaRequirementsDrafts parse(String summary, List<AiFindingDraft> findings,
			List<AssistantMessage> messages, Idea idea, int maxProposals) {
		List<Problem> problems = new ArrayList<>();
		Map<Kind, Map<String, String>> existing = existingNames(idea);
		String ideaText = idea.ideaText() == null ? "" : idea.ideaText().strip();

		Map<Key, Candidate> candidates = collect(findings == null ? List.of() : findings, existing,
				ideaText, Math.max(1, maxProposals), problems);

		Map<Key, List<Reference>> resolved = new LinkedHashMap<>();
		for (Map.Entry<Key, Candidate> entry : candidates.entrySet()) {
			resolved.put(entry.getKey(),
					resolve(entry.getKey(), entry.getValue(), candidates, existing, problems));
		}

		Set<Key> kept = new LinkedHashSet<>();
		for (Key key : candidates.keySet()) {
			if (needsActor(key.kind()) && firstActor(resolved.get(key)) < 0) {
				problems.add(new Problem(candidates.get(key).name(), true,
						"a " + key.kind().label() + " needs a primary actor"));
			} else {
				kept.add(key);
			}
		}

		List<Draft> drafts = new ArrayList<>();
		Map<String, Set<String>> goalEdges = new HashMap<>();
		for (Key key : kept) {
			Candidate candidate = candidates.get(key);
			List<Reference> references = finalReferences(key, candidate, resolved.get(key), kept,
					goalEdges, problems);
			drafts.add(new Draft(key.kind(), candidate.name(), candidate.text(), candidate.note(),
					candidate.assumption(), candidate.evidence(), candidate.evidenceUnverified(),
					candidate.confidence(), references));
		}
		return new IdeaRequirementsDrafts(summary, drafts, messages, problems, null);
	}

	/** The findings that are well-formed on their own, in order, keyed by identity. */
	private static Map<Key, Candidate> collect(List<AiFindingDraft> findings,
			Map<Kind, Map<String, String>> existing, String ideaText, int maxProposals,
			List<Problem> problems) {
		Map<Key, Candidate> candidates = new LinkedHashMap<>();
		for (AiFindingDraft finding : findings) {
			String name = boundedName(entityName(finding));
			String subject = name != null ? name : String.valueOf(finding.findingType());
			Kind kind = Kind.parse(finding.findingType());
			if (kind == null) {
				problems.add(new Problem(subject, true,
						"unknown entity kind '" + finding.findingType() + "'"));
				continue;
			}
			if (name == null) {
				problems.add(new Problem(subject, true, "a " + kind.label() + " without a name"));
				continue;
			}
			String text = DefinitionExecutorAssistant.boundedText(finding.suggestedIssueText());
			if (text == null) {
				problems.add(new Problem(name, true, "no text"));
				continue;
			}
			Key key = new Key(kind, key(name));
			if (existing.get(kind).containsKey(key.name())) {
				problems.add(new Problem(name, true,
						"the project already has a " + kind.label() + " with this name"));
				continue;
			}
			if (candidates.containsKey(key)) {
				problems.add(new Problem(name, true, "repeats an earlier " + kind.label()));
				continue;
			}
			if (candidates.size() >= maxProposals) {
				problems.add(new Problem(name, true,
						"over the limit of " + maxProposals + " proposals"));
				continue;
			}
			List<String> evidence = evidence(finding.evidenceReferences());
			boolean unverified = !evidence.isEmpty()
					&& EvidenceCheck.unverified(evidence, ideaText);
			if (unverified) {
				problems.add(new Problem(name, false,
						"cites evidence that is not in the idea text"));
			}
			String note = DefinitionExecutorAssistant.boundedText(finding.suggestedNoteText());
			candidates.put(key, new Candidate(name, text, note, isAssumption(note), evidence,
					unverified, confidence(finding.confidence()), participants(finding)));
		}
		return candidates;
	}

	/** One proposal's participants as references, in the model's order, without repeats. */
	private static List<Reference> resolve(Key self, Candidate candidate,
			Map<Key, Candidate> candidates, Map<Kind, Map<String, String>> existing,
			List<Problem> problems) {
		Set<Kind> allowed = ALLOWED_REFERENCES.get(self.kind());
		if (allowed.isEmpty()) {
			if (!candidate.participants().isEmpty()) {
				problems.add(new Problem(candidate.name(), false,
						"a " + self.kind().label() + " refers to nothing; its references were ignored"));
			}
			return List.of();
		}
		Set<Reference> references = new LinkedHashSet<>();
		for (String participant : candidate.participants()) {
			int colon = participant.indexOf(':');
			Kind kind = colon < 0 ? null : Kind.parse(participant.substring(0, colon));
			String name = colon < 0 ? "" : participant.substring(colon + 1).strip();
			if (kind == null || name.isEmpty()) {
				problems.add(new Problem(candidate.name(), false,
						"malformed reference '" + participant + "' left out"));
				continue;
			}
			if (!allowed.contains(kind)) {
				problems.add(new Problem(candidate.name(), false, "a " + self.kind().label()
						+ " cannot refer to a " + kind.label() + " ('" + participant + "' left out)"));
				continue;
			}
			Key target = new Key(kind, key(name));
			if (target.equals(self)) {
				continue;
			}
			Candidate proposed = candidates.get(target);
			String existingName = existing.get(kind).get(target.name());
			if (proposed != null) {
				references.add(new Reference(kind, proposed.name(), false));
			} else if (existingName != null) {
				references.add(new Reference(kind, existingName, true));
			} else {
				problems.add(new Problem(candidate.name(), false,
						"unknown " + kind.label() + " '" + name + "' left out"));
			}
		}
		return new ArrayList<>(references);
	}

	/**
	 * A kept proposal's references after dropped proposals are gone: dangling ones and goal
	 * cycles are left out, and the first actor goes to the front.
	 */
	private static List<Reference> finalReferences(Key self, Candidate candidate,
			List<Reference> resolved, Set<Key> kept, Map<String, Set<String>> goalEdges,
			List<Problem> problems) {
		List<Reference> references = new ArrayList<>();
		for (Reference reference : resolved) {
			Key target = new Key(reference.kind(), key(reference.name()));
			if (!reference.existing() && !kept.contains(target)) {
				problems.add(new Problem(candidate.name(), false, "'" + reference.name()
						+ "' was dropped, so the reference to it was left out"));
				continue;
			}
			if (!reference.existing() && self.kind() == Kind.GOAL && reference.kind() == Kind.GOAL) {
				if (reaches(goalEdges, target.name(), self.name())) {
					problems.add(new Problem(candidate.name(), false, "the reference to goal '"
							+ reference.name() + "' would make a goal part of itself; left out"));
					continue;
				}
				goalEdges.computeIfAbsent(self.name(), k -> new HashSet<>()).add(target.name());
			}
			references.add(reference);
		}
		int actor = firstActor(references);
		if (actor > 0) {
			references.add(0, references.remove(actor));
		}
		return references;
	}

	/** True when {@code to} can be reached from {@code from} along the goal edges. */
	private static boolean reaches(Map<String, Set<String>> edges, String from, String to) {
		Set<String> seen = new HashSet<>();
		Deque<String> pending = new ArrayDeque<>();
		pending.push(from);
		while (!pending.isEmpty()) {
			String current = pending.pop();
			if (current.equals(to)) {
				return true;
			}
			if (seen.add(current)) {
				edges.getOrDefault(current, Set.of()).forEach(pending::push);
			}
		}
		return false;
	}

	private static boolean needsActor(Kind kind) {
		return kind == Kind.STORY || kind == Kind.USE_CASE;
	}

	private static int firstActor(List<Reference> references) {
		for (int i = 0; i < references.size(); i++) {
			if (references.get(i).kind() == Kind.ACTOR) {
				return i;
			}
		}
		return -1;
	}

	private static Map<Kind, Map<String, String>> existingNames(Idea idea) {
		Map<Kind, Map<String, String>> existing = new EnumMap<>(Kind.class);
		for (Kind kind : Kind.values()) {
			existing.put(kind, new HashMap<>());
		}
		addNames(existing.get(Kind.GOAL), idea.existingGoals());
		addNames(existing.get(Kind.ACTOR), idea.existingActors());
		addNames(existing.get(Kind.GLOSSARY_TERM), idea.existingGlossaryTerms());
		return existing;
	}

	private static void addNames(Map<String, String> into, List<String> names) {
		if (names == null) {
			return;
		}
		for (String name : names) {
			String display = boundedName(name);
			if (display != null) {
				into.putIfAbsent(key(display), display);
			}
		}
	}

	private static String entityName(AiFindingDraft finding) {
		Object named = finding.metadata() == null ? null
				: finding.metadata().get(ReviewResultMapper.SUGGESTED_ENTITY_NAME);
		return named == null ? null : named.toString();
	}

	private static List<String> participants(AiFindingDraft finding) {
		List<String> participants = new ArrayList<>();
		Object named = finding.metadata() == null ? null
				: finding.metadata().get(ReviewResultMapper.PARTICIPANTS);
		if (named instanceof List<?> list) {
			for (Object item : list) {
				if (item != null && !item.toString().isBlank()) {
					participants.add(item.toString().strip());
				}
			}
		}
		return participants;
	}

	private static List<String> evidence(List<String> references) {
		List<String> evidence = new ArrayList<>();
		if (references != null) {
			for (String reference : references) {
				String bounded = DefinitionExecutorAssistant.boundedText(reference);
				if (bounded != null && evidence.size() < MAX_EVIDENCE) {
					evidence.add(bounded);
				}
			}
		}
		return evidence;
	}

	private static boolean isAssumption(String note) {
		return note != null && note.toLowerCase(Locale.ROOT).startsWith(ASSUMPTION_PREFIX);
	}

	/** In 0..1, or null when the model gave none or not a number. */
	private static Double confidence(Double confidence) {
		if (confidence == null || confidence.isNaN()) {
			return null;
		}
		return Math.max(0.0, Math.min(1.0, confidence));
	}

	/** Single-line, at most {@value #MAX_NAME_LENGTH} characters; null when blank. */
	private static String boundedName(String name) {
		if (name == null) {
			return null;
		}
		String oneLine = name.replaceAll("\\s+", " ").strip();
		if (oneLine.isEmpty()) {
			return null;
		}
		return oneLine.length() <= MAX_NAME_LENGTH ? oneLine : oneLine.substring(0, MAX_NAME_LENGTH);
	}

	/** A name's identity: case, spacing and a trailing plural "s" don't count. */
	static String key(String name) {
		String key = name.replaceAll("\\s+", " ").strip().toLowerCase(Locale.ROOT);
		if (key.length() > 3 && key.endsWith("s") && !key.endsWith("ss")) {
			key = key.substring(0, key.length() - 1);
		}
		return key;
	}
}
