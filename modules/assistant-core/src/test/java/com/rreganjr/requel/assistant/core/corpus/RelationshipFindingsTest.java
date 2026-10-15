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

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.CommandBackedAssistantResultApplicator;

/** Issue #266: a relationship finding as one action per participant, sharing one issue. */
class RelationshipFindingsTest {

	private static final EntityRef GOAL_1 = EntityRef.of("Goal", 1L);
	private static final EntityRef GOAL_12 = EntityRef.of("Goal", 12L);
	private static final EntityRef STORY_3 = EntityRef.of("Story", 3L);

	@Test
	void theShareKeyIgnoresParticipantOrder() {
		assertThat(RelationshipFindings.shareKey("X", List.of(GOAL_12, STORY_3, GOAL_1)))
				.isEqualTo(RelationshipFindings.shareKey("X", List.of(STORY_3, GOAL_1, GOAL_12)))
				.startsWith("X-").hasSize(2 + 16);
		assertThat(RelationshipFindings.shareKey("X", List.of(GOAL_1, GOAL_12)))
				.isNotEqualTo(RelationshipFindings.shareKey("Y", List.of(GOAL_1, GOAL_12)))
				.isNotEqualTo(RelationshipFindings.shareKey("X", List.of(GOAL_1, STORY_3)));
	}

	@Test
	void oneSharedIssueActionPerParticipant() {
		List<AnnotationAction> actions = RelationshipFindings.issueActions("corpus-finder",
				"POSSIBLE_CONFLICT", "Possible conflict", "LOW", false, List.of(STORY_3, GOAL_1),
				Map.of(GOAL_1, "fp-goal"), Map.of("score", 0.4));
		assertThat(actions).hasSize(2);
		String shareKey = RelationshipFindings.shareKey("POSSIBLE_CONFLICT", List.of(GOAL_1, STORY_3));
		assertThat(actions).extracting(AnnotationAction::targetRef).containsExactly(GOAL_1, STORY_3);
		assertThat(actions.get(0).actionKey()).isEqualTo("corpus-finder:Goal:1:" + shareKey);
		for (AnnotationAction action : actions) {
			assertThat(action.actionType()).isEqualTo(AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE);
			assertThat(action.metadata()).containsEntry("scope", "PROJECT")
					.containsEntry("shareKey", shareKey)
					.containsEntry(CommandBackedAssistantResultApplicator.STALE_TOGETHER, true)
					.containsEntry("findingType", "POSSIBLE_CONFLICT")
					.containsEntry("mustResolve", false)
					.containsEntry("participants", List.of("Goal:1", "Story:3"))
					.containsEntry("score", 0.4);
		}
		assertThat(actions.get(0).metadata())
				.containsEntry(CommandBackedAssistantResultApplicator.TARGET_FINGERPRINT, "fp-goal");
		assertThat(actions.get(1).metadata())
				.doesNotContainKey(CommandBackedAssistantResultApplicator.TARGET_FINGERPRINT);
	}

	@Test
	void aRelationshipNeedsTwoParticipants() {
		assertThatThrownBy(() -> RelationshipFindings.issueActions("a", "T", "t", "LOW", false,
				List.of(GOAL_1, GOAL_1), Map.of(), Map.of()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void labelsReadAsTypeAndName() {
		assertThat(RelationshipFindings.label(EntityRef.of("UseCase", 4L), "Renew a loan"))
				.isEqualTo("Use case \"Renew a loan\"");
		assertThat(RelationshipFindings.label(GOAL_1, null)).isEqualTo("Goal \"#1\"");
	}
}
