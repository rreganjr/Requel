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
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.core.CommandBackedAssistantResultApplicator;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Kind;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Member;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.ScoredPair;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Settings;
import com.rreganjr.requel.assistant.core.corpus.CorpusMembers.CorpusSet;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.SwitchableAssistantCatalog;
import com.rreganjr.requel.project.UseCase;

/**
 * Issue #266, stage 1: "Find overlaps". Runs the {@link CorpusCandidateFinder} over a set (a
 * project, or a goal or use case with what hangs off it) and raises each candidate pair as one
 * advisory issue on both entities: a possible conflict, with what each side says, or a possible
 * overlap. No model; serves only {@link #TASK_TYPE}, which is only ever dispatched by hand, so an
 * edit never runs it.
 */
@Component
public class CorpusFinderAssistant implements RequelAssistant<Object> {

	public static final String ASSISTANT_ID = "corpus-finder";
	public static final String TASK_TYPE = "CORPUS_CANDIDATES";

	public static final String POSSIBLE_CONFLICT = "POSSIBLE_CONFLICT";
	public static final String POSSIBLE_OVERLAP = "POSSIBLE_OVERLAP";

	private final ObjectProvider<WordRelations> wordRelations;
	private final Settings settings;

	@Autowired
	public CorpusFinderAssistant(ObjectProvider<WordRelations> wordRelations,
			@Value("${requel.corpus.finder.overlap-threshold:0.35}") double overlapThreshold,
			@Value("${requel.corpus.finder.conflict-threshold:0.20}") double conflictThreshold,
			@Value("${requel.corpus.finder.synonym-weight:0.5}") double synonymWeight) {
		this.wordRelations = wordRelations;
		this.settings = new Settings(overlapThreshold, conflictThreshold, synonymWeight);
	}

	@Override
	public String assistantId() {
		return ASSISTANT_ID;
	}

	@Override
	public Class<Object> targetType() {
		return Object.class;
	}

	@Override
	public boolean handlesTask(String taskType) {
		return TASK_TYPE.equals(taskType);
	}

	@Override
	public CleanupPolicy cleanupPolicy() {
		return CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED;
	}

	@Override
	public String displayName() {
		return "Find overlaps";
	}

	@Override
	public String group() {
		return SwitchableAssistantCatalog.CORPUS;
	}

	public Settings settings() {
		return settings;
	}

	@Override
	public AssistantResult analyze(AssistantContext context, Object target)
			throws AssistantException {
		Project project = projectOf(target);
		if (project == null) {
			throw new AssistantException("Find overlaps runs on a project, a goal or a use case, not "
					+ target.getClass().getSimpleName());
		}
		CorpusSet set = CorpusMembers.of(target, project);
		CorpusCandidateFinder finder = new CorpusCandidateFinder(
				wordRelations.getIfAvailable(() -> WordRelations.NONE), settings);
		List<ScoredPair> candidates = finder.find(set);
		List<AnnotationAction> actions = new ArrayList<>();
		int conflicts = 0;
		for (ScoredPair pair : candidates) {
			Member a = set.member(pair.a());
			Member b = set.member(pair.b());
			boolean conflict = pair.kind() == Kind.CONFLICT;
			conflicts += conflict ? 1 : 0;
			actions.addAll(RelationshipFindings.issueActions(ASSISTANT_ID,
					conflict ? POSSIBLE_CONFLICT : POSSIBLE_OVERLAP, text(pair, a, b), "LOW", false,
					List.of(pair.a(), pair.b()), set.fingerprints(),
					Map.of("score", pair.score())));
		}
		AssistantResult.Builder result = AssistantResult.builder().assistantId(ASSISTANT_ID)
				.runId(context.runId())
				.summary(set.members().size() + " entities; " + conflicts + " possible conflicts, "
						+ (candidates.size() - conflicts) + " possible overlaps");
		for (AnnotationAction action : actions) {
			result.annotationAction(action);
		}
		return result.metadata(Map.of(CommandBackedAssistantResultApplicator.CORPUS_SCOPE, set.scope(),
						"members", set.members().size(), "candidates", candidates.size()))
				.build();
	}

	static String text(ScoredPair pair, Member a, Member b) {
		String between = RelationshipFindings.label(pair.a(), a.name()) + " and "
				+ RelationshipFindings.label(pair.b(), b.name());
		if (pair.kind() == Kind.CONFLICT) {
			StringBuilder text = new StringBuilder("Possible conflict between ").append(between)
					.append(": ");
			for (int i = 0; i < pair.hints().size(); i++) {
				text.append(i == 0 ? "" : "; ").append(pair.hints().get(i).describe());
			}
			return text.append(". Check that both can hold.").toString();
		}
		return "Possible overlap between " + between + " (similarity "
				+ String.format(java.util.Locale.ROOT, "%.2f", pair.score())
				+ "). Check whether one repeats or refines the other.";
	}

	private static Project projectOf(Object target) {
		ProjectOrDomain owner = null;
		if (target instanceof Project project) {
			return project;
		}
		if (target instanceof Goal goal) {
			owner = goal.getProjectOrDomain();
		} else if (target instanceof UseCase useCase) {
			owner = useCase.getProjectOrDomain();
		}
		return owner instanceof Project project ? project : null;
	}

	/** The reference of a root, for callers that dispatch. */
	public static EntityRef rootRef(Object root) {
		if (root instanceof Project project) {
			return EntityRef.of("Project", project.getId());
		}
		return CorpusMembers.ref(root);
	}
}
