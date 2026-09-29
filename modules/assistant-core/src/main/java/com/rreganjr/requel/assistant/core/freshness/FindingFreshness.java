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
package com.rreganjr.requel.assistant.core.freshness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.spi.AnnotationFreshness;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingRepository;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingState;
import com.rreganjr.requel.project.ProjectOrDomainEntity;

/**
 * Issue #270: staleness from the assistant findings. An annotation is stale on an entity when at
 * least one open ({@code ACTIVE} or {@code SUPERSEDED}) finding links it to that entity and every
 * such finding is stale: {@code SUPERSEDED} (a later run no longer reports it but a human's
 * discussion kept the annotation), or {@code ACTIVE} with a recorded fingerprint that differs from
 * the entity's current {@link TargetFingerprint}. A fresh finding from any assistant means the
 * annotation still applies. A finding recorded before #270 (no fingerprint) is not stale, and an
 * annotation with no finding (a person's, or the old lexical path's) never is.
 */
@Component
public class FindingFreshness implements AnnotationFreshness {

	private static final List<String> OPEN_STATES = List.of(AssistantFindingState.ACTIVE.name(),
			AssistantFindingState.SUPERSEDED.name());

	private final AssistantFindingRepository findingRepository;

	@Autowired
	public FindingFreshness(AssistantFindingRepository findingRepository) {
		this.findingRepository = Objects.requireNonNull(findingRepository, "findingRepository");
	}

	@Override
	public StaleAnnotations staleAnnotations(Collection<? extends Annotatable> annotatables) {
		if (annotatables == null || annotatables.isEmpty()) {
			return StaleAnnotations.NONE;
		}
		Set<Long> annotationIds = new HashSet<>();
		for (Annotatable annotatable : annotatables) {
			if (targetKey(annotatable) == null || annotatable.getAnnotations() == null) {
				continue;
			}
			for (Annotation annotation : annotatable.getAnnotations()) {
				if (annotation.getId() != null) {
					annotationIds.add(annotation.getId());
				}
			}
		}
		if (annotationIds.isEmpty()) {
			return StaleAnnotations.NONE;
		}
		// target key + annotation id -> the open findings linking them
		Map<String, List<AssistantFindingEntity>> findings = new HashMap<>();
		for (AssistantFindingEntity finding : findingRepository
				.findByAppliedAnnotationIdInAndStateIn(annotationIds, OPEN_STATES)) {
			findings.computeIfAbsent(
					key(finding.getTargetType(), finding.getTargetId(),
							finding.getAppliedAnnotationId()),
					k -> new ArrayList<>()).add(finding);
		}
		if (findings.isEmpty()) {
			return StaleAnnotations.NONE;
		}
		Set<String> stale = new HashSet<>();
		for (Annotatable annotatable : annotatables) {
			String target = targetKey(annotatable);
			if (target == null || annotatable.getAnnotations() == null) {
				continue;
			}
			String current = null;
			boolean fingerprinted = false;
			for (Annotation annotation : annotatable.getAnnotations()) {
				List<AssistantFindingEntity> linked = findings.get(target + "#" + annotation.getId());
				if (linked == null) {
					continue;
				}
				if (!fingerprinted) {
					current = TargetFingerprint.of(annotatable);
					fingerprinted = true;
				}
				if (allStale(linked, current)) {
					stale.add(target + "#" + annotation.getId());
				}
			}
		}
		if (stale.isEmpty()) {
			return StaleAnnotations.NONE;
		}
		return (annotatable, annotation) -> {
			String target = targetKey(annotatable);
			return target != null && annotation != null
					&& stale.contains(target + "#" + annotation.getId());
		};
	}

	private static boolean allStale(List<AssistantFindingEntity> linked, String current) {
		for (AssistantFindingEntity finding : linked) {
			if (!isStale(finding, current)) {
				return false;
			}
		}
		return true;
	}

	static boolean isStale(AssistantFindingEntity finding, String current) {
		if (AssistantFindingState.SUPERSEDED.name().equals(finding.getState())) {
			return true;
		}
		String recorded = finding.getTargetFingerprint();
		return recorded != null && current != null && !recorded.equals(current);
	}

	/** The finding target key of an entity, as the dispatcher writes it, or null. */
	private static String targetKey(Annotatable annotatable) {
		if (annotatable instanceof ProjectOrDomainEntity entity && entity.getId() != null
				&& entity.getProjectOrDomainEntityInterface() != null) {
			return entity.getProjectOrDomainEntityInterface().getSimpleName() + ":"
					+ entity.getId();
		}
		return null;
	}

	private static String key(String targetType, Long targetId, Long annotationId) {
		return targetType + ":" + targetId + "#" + annotationId;
	}
}
