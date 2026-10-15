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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantMessage;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.CommandBackedAssistantResultApplicator;
import com.rreganjr.requel.assistant.core.StagedAssistant;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.ScoredPair;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Settings;
import com.rreganjr.requel.assistant.core.corpus.CorpusFinderAssistant;
import com.rreganjr.requel.assistant.core.corpus.CorpusMembers;
import com.rreganjr.requel.assistant.core.corpus.CorpusMembers.CorpusSet;
import com.rreganjr.requel.assistant.core.corpus.CorpusPack;
import com.rreganjr.requel.assistant.core.corpus.CorpusPackBuilder;
import com.rreganjr.requel.assistant.core.corpus.CorpusPackBuilder.Built;
import com.rreganjr.requel.assistant.core.corpus.CorpusPackBuilder.CorpusTooLargeException;
import com.rreganjr.requel.assistant.core.corpus.RelationshipFindings;
import com.rreganjr.requel.assistant.core.corpus.WordRelations;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.DefinitionBacked;
import com.rreganjr.requel.assistant.core.definition.VocabularyEntry;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.SwitchableAssistantCatalog;
import com.rreganjr.requel.project.UseCase;

/**
 * Issue #266, stage 2: runs a {@code CORPUS} definition over a set. One provider call: the
 * {@link CorpusPack} (an index of the set, then the finder's candidate pairs in full) with the
 * definition's instructions, answered in {@code CorpusReviewOutput} v1, where each finding names
 * its participants. Each finding becomes one issue on every participant
 * ({@link RelationshipFindings}).
 *
 * <ul>
 * <li>A set whose index is over {@code requel.ai.corpus.max-input-chars} is refused: the run
 * fails with the overflow named, and nothing is analysed or cleaned up.</li>
 * <li>A participant outside the set is dropped and counted; a finding left with fewer than two is
 * dropped. Evidence is checked against the participants' text as sent.</li>
 * <li>Findings with the same type and participants are merged, so a relationship is raised once.
 * </li>
 * <li>The finder's advisory issues on the pairs this run judged are replaced.</li>
 * </ul>
 */
public class CorpusAnalysisAssistant
		implements RequelAssistant<Object>, DefinitionBacked, StagedAssistant {

	private static final Logger log = LoggerFactory.getLogger(CorpusAnalysisAssistant.class);

	/** Result metadata: participants named that aren't in the set. */
	public static final String UNRESOLVED_PARTICIPANTS = "unresolvedParticipants";

	private final AssistantDefinition definition;
	private final AiDefinitionExecutorFactory runtime;
	private final DefinitionExecutorAssistant base;

	CorpusAnalysisAssistant(AssistantDefinition definition, AiDefinitionExecutorFactory runtime) {
		this.definition = definition;
		this.runtime = runtime;
		this.base = new DefinitionExecutorAssistant(definition, runtime);
	}

	@Override
	public AssistantDefinition definition() {
		return definition;
	}

	@Override
	public String assistantId() {
		return definition.key();
	}

	@Override
	public String displayName() {
		return definition.displayName();
	}

	@Override
	public Class<Object> targetType() {
		return Object.class;
	}

	@Override
	public boolean handlesTask(String taskType) {
		return definition.taskType().equals(taskType);
	}

	@Override
	public boolean projectSwitchable() {
		return true;
	}

	@Override
	public String group() {
		return SwitchableAssistantCatalog.CORPUS;
	}

	/** Like reviews (#360): a re-run removes the untouched issues it no longer reports. */
	@Override
	public CleanupPolicy cleanupPolicy() {
		return CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED;
	}

	@Override
	public AssistantResult analyze(AssistantContext context, Object target)
			throws AssistantException {
		return prepare(context, target).complete().get(0);
	}

	/**
	 * #363: build the set, find the candidates and build the pack inside the analyze transaction;
	 * the stage makes the call and maps the findings from that detached data.
	 */
	@Override
	public Stage prepare(AssistantContext context, Object target) throws AssistantException {
		String skipReason = base.skipReason(context);
		if (skipReason != null) {
			AssistantResult skipped = AssistantResult.builder().assistantId(assistantId())
					.runId(context.runId()).summary(skipReason).build();
			return () -> List.of(skipped);
		}
		Project project = projectOf(target);
		if (project == null || runtime.corpusPackBuilder == null) {
			throw new AssistantException("A corpus analysis runs on a project, a goal or a use case"
					+ (runtime.corpusPackBuilder == null ? " (no corpus pack builder)" : ""));
		}
		CorpusMembers.SetKind kind = CorpusMembers.SetKind.of(target);
		CorpusSet set = CorpusMembers.of(target, project);
		EntityRef root = CorpusFinderAssistant.rootRef(target);
		AiProperties.Corpus limits = runtime.aiProperties.getCorpus();

		// the finder's pairs, at the lower overlap threshold: the model judges each one
		Settings finderSettings = runtime.corpusFinderSettings();
		Settings settings = new Settings(limits.getCandidateOverlapThreshold(),
				finderSettings.conflictThreshold(), finderSettings.synonymWeight());
		WordRelations relations = runtime.wordRelations();
		List<ScoredPair> candidates = new CorpusCandidateFinder(relations, settings).find(set);

		Built built;
		try {
			built = runtime.corpusPackBuilder.build(set, kind.name(), root,
					label(root), project.getId(), candidates, limits.getMaxInputChars(),
					limits.getIndexTextChars());
		} catch (CorpusTooLargeException e) {
			throw new AssistantException(e.getMessage(), e);
		}
		CorpusPack pack = built.pack();
		if (runtime.runStore != null && !pack.notes().redacted().isEmpty()) {
			try {
				runtime.runStore.recordRedactions(context.runId(), pack.notes().redacted().size(),
						List.of());
			} catch (RuntimeException e) {
				log.warn("Failed to record redactions for run {}: {}", context.runId(),
						e.getMessage());
			}
		}

		AiAnalysisRequest request = new AiAnalysisRequest(assistantId(), context.runId(),
				definition.taskType(), root, context.projectRef(), context.locale(),
				List.of(pack), definition.outputSchemaName(), definition.outputSchemaVersion(),
				outputSchema(set), base.dataHandlingFlags(context.projectRef()),
				base.attributes(context), base.instructions());
		Map<EntityRef, String> sentText = built.sentText();
		return () -> {
			AiAnalysisResponse response;
			try {
				response = runtime.aiAnalysisClient.analyze(request);
			} catch (AiAnalysisException e) {
				log.warn("Corpus analysis {} failed for run {}: {}", definition.key(),
						context.runId(), e.getMessage(), e);
				throw new AssistantException("AI corpus analysis failed: " + e.getMessage(), e);
			}
			base.persistUsage(context.runId(), response.usage());
			return List.of(map(context, set, pack, sentText, candidates, response));
		};
	}

	AssistantResult map(AssistantContext context, CorpusSet set, CorpusPack pack,
			Map<EntityRef, String> sentText, List<ScoredPair> candidates,
			AiAnalysisResponse response) {
		AssistantResult.Builder result = AssistantResult.builder().assistantId(assistantId())
				.runId(context.runId()).summary(summary(response.summary(), pack.notes()));
		if (response.messages() != null) {
			response.messages().forEach(result::message);
		}
		Map<String, EntityRef> members = new HashMap<>();
		for (CorpusMembersRef member : CorpusMembersRef.of(set)) {
			members.put(member.ref(), member.entityRef());
		}
		int unresolved = 0;
		int unverified = 0;
		int vocabularyMisses = 0;
		int dropped = 0;
		Set<String> raised = new LinkedHashSet<>();
		List<AiFindingDraft> drafts = response.findings() == null ? List.of() : response.findings();
		for (AiFindingDraft finding : drafts) {
			Set<EntityRef> participants = new LinkedHashSet<>();
			Object named = finding.metadata().get(ReviewResultMapper.PARTICIPANTS);
			if (named instanceof List<?> list) {
				for (Object item : list) {
					EntityRef ref = item == null ? null : members.get(item.toString().strip());
					if (ref == null) {
						unresolved++;
					} else {
						participants.add(ref);
					}
				}
			}
			String text = DefinitionExecutorAssistant.boundedText(finding.suggestedIssueText());
			if (participants.size() < 2 || text == null) {
				dropped++;
				continue;
			}
			List<EntityRef> sorted = RelationshipFindings.sorted(participants);
			String shareKey = RelationshipFindings.shareKey(finding.findingType(), sorted);
			if (!raised.add(shareKey)) {
				// the same relationship again: raised once
				continue;
			}
			if (base.vocabularyEntry(finding.findingType()) == null) {
				vocabularyMisses++;
			}
			StringBuilder participantText = new StringBuilder();
			for (EntityRef ref : sorted) {
				participantText.append(sentText.getOrDefault(ref, "")).append('\n');
			}
			if (EvidenceCheck.unverified(finding.evidenceReferences(), participantText.toString())) {
				unverified++;
			}
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put(DefinitionExecutorAssistant.DEFINITION_KEY, definition.key());
			metadata.put(DefinitionExecutorAssistant.DEFINITION_VERSION, definition.version());
			metadata.put(DefinitionExecutorAssistant.CATEGORY, VocabularyEntry.QUALITY);
			List<AnnotationAction> actions = RelationshipFindings.issueActions(assistantId(),
					finding.findingType(), text, finding.severity(), true, sorted,
					set.fingerprints(), metadata);
			actions = withEvidence(actions, finding);
			actions.forEach(result::annotationAction);
			String issueKey = actions.get(0).actionKey();
			for (String position : finding.suggestedPositions()) {
				String positionText = DefinitionExecutorAssistant.boundedText(position);
				if (positionText != null) {
					result.annotationAction(new AnnotationAction(
							issueKey + ":pos:" + DefinitionExecutorAssistant.hash(positionText),
							AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, issueKey,
							positionText, null, null, List.of(), Map.of()));
				}
			}
		}

		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put(AssistantRunWorker.EVIDENCE_UNVERIFIED, unverified);
		metadata.put(AssistantRunWorker.VOCABULARY_MISSES, vocabularyMisses);
		metadata.put(UNRESOLVED_PARTICIPANTS, unresolved);
		metadata.put(CommandBackedAssistantResultApplicator.CORPUS_SCOPE, set.scope());
		metadata.put(CommandBackedAssistantResultApplicator.RETIRES_FINDINGS,
				finderKeys(pack, candidates));
		metadata.put("members", pack.notes().members());
		metadata.put("candidatesFound", pack.notes().candidatesFound());
		metadata.put("candidatesSent", pack.notes().candidatesSent());
		metadata.put("partial", pack.notes().partial());
		metadata.put("inputChars", pack.notes().totalChars());
		result.metadata(metadata);
		if (pack.notes().partial()) {
			result.message(AssistantMessage.warning("Sent " + pack.notes().candidatesSent() + " of "
					+ pack.notes().candidatesFound() + " candidate pairs; the rest were not"
					+ " analysed (requel.ai.corpus.max-input-chars)"));
		}
		if (unresolved + dropped > 0) {
			result.message(AssistantMessage.warning(unresolved + " participant(s) not in the set; "
					+ dropped + " finding(s) dropped for naming fewer than two"));
		}
		if (unverified > 0) {
			result.message(AssistantMessage.warning(unverified
					+ " finding(s) cite evidence that is not in the participants' text"));
		}
		if (vocabularyMisses > 0) {
			result.message(AssistantMessage.warning(vocabularyMisses
					+ " finding(s) use a type outside the definition's vocabulary"));
		}
		return result.build();
	}

	/** The run summary: the model's, and a partial run says so. */
	static String summary(String modelSummary, CorpusPack.Notes notes) {
		String summary = modelSummary == null ? "" : modelSummary.strip();
		if (notes.partial()) {
			summary = "Partial: sent " + notes.candidatesSent() + " of " + notes.candidatesFound()
					+ " candidate pairs. " + summary;
		}
		return summary;
	}

	/** The finder's action keys for each pair this run sent: its advisory issues there retire. */
	private static List<String> finderKeys(CorpusPack pack, List<ScoredPair> candidates) {
		List<String> keys = new ArrayList<>();
		int sent = pack.notes().candidatesSent();
		for (int i = 0; i < sent && i < candidates.size(); i++) {
			ScoredPair pair = candidates.get(i);
			for (String type : List.of(CorpusFinderAssistant.POSSIBLE_CONFLICT,
					CorpusFinderAssistant.POSSIBLE_OVERLAP)) {
				String shareKey = RelationshipFindings.shareKey(type, List.of(pair.a(), pair.b()));
				keys.add(RelationshipFindings.actionKey(CorpusFinderAssistant.ASSISTANT_ID,
						pair.a(), shareKey));
				keys.add(RelationshipFindings.actionKey(CorpusFinderAssistant.ASSISTANT_ID,
						pair.b(), shareKey));
			}
		}
		return keys;
	}

	private static List<AnnotationAction> withEvidence(List<AnnotationAction> actions,
			AiFindingDraft finding) {
		var evidence = DefinitionExecutorAssistant.evidenceRefs(finding.evidenceReferences());
		if (evidence.isEmpty()) {
			return actions;
		}
		List<AnnotationAction> with = new ArrayList<>();
		for (AnnotationAction action : actions) {
			with.add(new AnnotationAction(action.actionKey(), action.actionType(),
					action.targetRef(), action.parentActionKey(), action.text(), action.severity(),
					finding.confidence(), evidence, action.metadata()));
		}
		return with;
	}

	/** The output schema with the participants restricted to the set's members. */
	JsonNode outputSchema(CorpusSet set) {
		JsonNode schema = base.outputSchema().deepCopy();
		JsonNode items = schema.path("properties").path("findings").path("items")
				.path("properties").path("participants").path("items");
		if (items instanceof ObjectNode node) {
			ArrayNode values = node.putArray("enum");
			for (String ref : set.scope()) {
				values.add(ref);
			}
		}
		return schema;
	}

	private static String label(EntityRef root) {
		return ("UseCase".equals(root.entityType()) ? "Use case" : root.entityType()) + " "
				+ root.entityId();
	}

	private static Project projectOf(Object target) {
		if (target instanceof Project project) {
			return project;
		}
		ProjectOrDomain owner = target instanceof Goal goal ? goal.getProjectOrDomain()
				: target instanceof UseCase useCase ? useCase.getProjectOrDomain() : null;
		return owner instanceof Project project ? project : null;
	}

	/** A member's {@code Type:id} and its reference. */
	private record CorpusMembersRef(String ref, EntityRef entityRef) {
		static List<CorpusMembersRef> of(CorpusSet set) {
			List<CorpusMembersRef> refs = new ArrayList<>();
			for (var member : set.members()) {
				refs.add(new CorpusMembersRef(CorpusPackBuilder.ref(member.ref()), member.ref()));
			}
			return refs;
		}
	}
}
