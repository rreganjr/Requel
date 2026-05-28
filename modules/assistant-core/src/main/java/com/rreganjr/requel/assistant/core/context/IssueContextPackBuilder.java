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
package com.rreganjr.requel.assistant.core.context;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;

/**
 * Builds an {@link IssueContextPack} for a target entity. Surfaces the
 * target's own unresolved (or recently resolved) issues plus other open
 * issues from the wider project so the assistant can spot correlations.
 */
@Component
public class IssueContextPackBuilder {

	private final RedactionPolicy redactionPolicy;
	private final ContextPackSizeLimits limits;
	private final Clock clock;

	public IssueContextPackBuilder(RedactionPolicy redactionPolicy,
			ContextPackSizeLimits limits) {
		this(redactionPolicy, limits, Clock.systemUTC());
	}

	IssueContextPackBuilder(RedactionPolicy redactionPolicy, ContextPackSizeLimits limits,
			Clock clock) {
		this.redactionPolicy = Objects.requireNonNull(redactionPolicy, "redactionPolicy");
		this.limits = Objects.requireNonNull(limits, "limits");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	public IssueContextPack build(Project project, Object target) {
		Objects.requireNonNull(project, "project");
		Objects.requireNonNull(target, "target");
		List<String> redacted = new ArrayList<>();
		List<String> truncated = new ArrayList<>();
		ContextPackBudget budget = new ContextPackBudget(limits.getMaxTotalCharacters());
		int maxField = limits.getMaxTextCharsPerField();

		EntityRef targetRef = entityRefFor(target);
		List<IssueSnapshot> targetIssues = new ArrayList<>();
		if (target instanceof Annotatable annotatable) {
			collectIssues(annotatable, targetRef, targetIssues, maxField, redacted, truncated, budget,
					Integer.MAX_VALUE);
		}

		int maxOpen = limits.getMaxProjectOpenIssues();
		List<IssueSnapshot> projectOpenIssues = new ArrayList<>();
		EntityRef projectRef = EntityRef.of("Project", project.getId());

		// Project-level issues first (the project is itself Annotatable).
		if (target != project) {
			collectIssues(project, projectRef, projectOpenIssues, maxField, redacted, truncated,
					budget, maxOpen);
		}

		for (ProjectOrDomainEntity entity : project.getProjectEntities()) {
			if (entity == target) {
				continue;
			}
			int remaining = maxOpen - projectOpenIssues.size();
			if (remaining <= 0) {
				truncated.add("projectOpenIssues capped at " + maxOpen);
				break;
			}
			EntityRef entityRef = EntityRef.of(simpleType(entity), entity.getId());
			boolean capped = collectIssues(entity, entityRef, projectOpenIssues, maxField,
					redacted, truncated, budget, remaining);
			if (capped) {
				break;
			}
		}

		ContextPackMetadata metadata = new ContextPackMetadata(Instant.now(clock),
				budget.totalCharacters(), !truncated.isEmpty(), redacted, truncated);
		return new IssueContextPack(targetRef, targetIssues, projectOpenIssues, metadata);
	}

	/**
	 * @return {@code true} when the per-pack {@code maxOpen} cap was reached,
	 *         signalling the caller to stop collecting.
	 */
	private boolean collectIssues(Annotatable source, EntityRef sourceRef,
			List<IssueSnapshot> sink, int maxField, List<String> redacted, List<String> truncated,
			ContextPackBudget budget, int remaining) {
		for (Annotation annotation : source.getAnnotations()) {
			if (sink.size() >= remaining) {
				return true;
			}
			if (budget.exceeded()) {
				truncated.add("issues list truncated by total-character budget");
				return true;
			}
			if (!(annotation instanceof Issue issue)) {
				continue;
			}
			if (issue.isResolved()) {
				continue;
			}
			String text = ContextPackTextUtils.prepareText("issue.text", issue.getText(), maxField,
					redactionPolicy, redacted, truncated);
			List<PositionSnapshot> positions = new ArrayList<>();
			for (Position position : issue.getPositions()) {
				String positionText = ContextPackTextUtils.prepareText("issue.position.text",
						position.getText(), maxField, redactionPolicy, redacted, truncated);
				positions.add(new PositionSnapshot(positionText));
				budget.add(positionText);
			}
			sink.add(new IssueSnapshot(text, issue.isMustBeResolved(), issue.isResolved(), sourceRef,
					positions));
			budget.add(text);
		}
		return false;
	}

	private static EntityRef entityRefFor(Object target) {
		if (target instanceof Project project) {
			return EntityRef.of("Project", project.getId());
		}
		if (target instanceof ProjectOrDomainEntity entity) {
			return EntityRef.of(simpleType(entity), entity.getId());
		}
		throw new IllegalArgumentException(
				"Unsupported target type for IssueContextPack: " + target.getClass().getName());
	}

	private static String simpleType(ProjectOrDomainEntity entity) {
		Class<?> iface = entity.getProjectOrDomainEntityInterface();
		return iface != null ? iface.getSimpleName() : entity.getClass().getSimpleName();
	}
}
