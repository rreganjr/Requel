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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.CommandBackedAssistantResultApplicator;

/**
 * Issue #266: a finding about a relationship, as annotation actions. One issue, attached to every
 * participant: one {@code CREATE_OR_UPDATE_ISSUE} per participant, all marked
 * {@code scope=PROJECT} with the same {@code shareKey}, so #268's shared-issue path joins them.
 * Each participant gets its own finding row (its fingerprint, ignore and cleanup), flagged
 * {@code staleTogether}.
 *
 * <p>The share key is {@code <findingType>-<first 16 hex of sha-256(sorted participant refs)>}:
 * the same relationship gives the same key whatever order the participants come in, and a long
 * participant list still fits the idempotency key column.
 */
public final class RelationshipFindings {

	private RelationshipFindings() {
	}

	/** The participants, de-duplicated and sorted by type then id. */
	public static List<EntityRef> sorted(java.util.Collection<EntityRef> participants) {
		List<EntityRef> sorted = new ArrayList<>(new LinkedHashSet<>(participants));
		sorted.sort(Comparator.comparing(EntityRef::entityType)
				.thenComparing(EntityRef::entityId));
		return sorted;
	}

	/** The share key of a relationship finding. */
	public static String shareKey(String findingType, java.util.Collection<EntityRef> participants) {
		StringBuilder refs = new StringBuilder();
		for (EntityRef ref : sorted(participants)) {
			refs.append(ref.entityType()).append(':').append(ref.entityId()).append(';');
		}
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(refs.toString().getBytes(StandardCharsets.UTF_8));
			return findingType + "-" + HexFormat.of().formatHex(digest).substring(0, 16);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * One issue action per participant.
	 *
	 * @param fingerprints each participant's fingerprint as the run read it (missing ones fall back
	 *            to the entity at apply time)
	 * @param extraMetadata more metadata for every action (may be empty)
	 */
	public static List<AnnotationAction> issueActions(String assistantId, String findingType,
			String text, String severity, boolean mustResolve, List<EntityRef> participants,
			Map<EntityRef, String> fingerprints, Map<String, Object> extraMetadata) {
		List<EntityRef> sorted = sorted(participants);
		if (sorted.size() < 2) {
			throw new IllegalArgumentException("a relationship needs two participants");
		}
		String shareKey = shareKey(findingType, sorted);
		List<AnnotationAction> actions = new ArrayList<>();
		for (EntityRef participant : sorted) {
			Map<String, Object> metadata = new HashMap<>(extraMetadata);
			metadata.put("mustResolve", mustResolve);
			metadata.put("findingType", findingType);
			metadata.put("scope", "PROJECT");
			metadata.put("shareKey", shareKey);
			metadata.put(CommandBackedAssistantResultApplicator.STALE_TOGETHER, Boolean.TRUE);
			metadata.put("participants", refs(sorted));
			String fingerprint = fingerprints.get(participant);
			if (fingerprint != null) {
				metadata.put(CommandBackedAssistantResultApplicator.TARGET_FINGERPRINT, fingerprint);
			}
			actions.add(new AnnotationAction(actionKey(assistantId, participant, shareKey),
					AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, participant, null, text,
					severity, null, List.of(), metadata));
		}
		return actions;
	}

	/** The action key of one participant's row: {@code <assistantId>:<Type>:<id>:<shareKey>}. */
	public static String actionKey(String assistantId, EntityRef participant, String shareKey) {
		return assistantId + ":" + participant.entityType() + ":" + participant.entityId() + ":"
				+ shareKey;
	}

	/** {@code Type:id} for each participant, sorted. */
	public static List<String> refs(java.util.Collection<EntityRef> participants) {
		List<String> refs = new ArrayList<>();
		for (EntityRef ref : sorted(participants)) {
			refs.add(ref.entityType() + ":" + ref.entityId());
		}
		return refs;
	}

	/** {@code Goal "Two-week loans"}, for issue text. */
	public static String label(EntityRef ref, String name) {
		return label(ref.entityType()) + " \"" + (name == null || name.isBlank()
				? "#" + ref.entityId() : name.strip()) + "\"";
	}

	private static String label(String type) {
		return switch (type) {
		case "UseCase" -> "Use case";
		case "GlossaryTerm" -> "Glossary term";
		default -> type;
		};
	}
}
