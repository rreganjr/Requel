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

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Argument;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;

/**
 * Issue #270: who an annotation belongs to, for deciding what an assistant may retire and what
 * reaches a context pack.
 */
public final class MachineAnnotations {

	private static final String ASSISTANT_SOURCE_PREFIX = "ASSISTANT:";

	private MachineAnnotations() {
	}

	/** @return true if the annotation carries an {@code ASSISTANT:<id>} source label. */
	public static boolean isAssistantSourced(Annotation annotation) {
		String source = annotation == null ? null : annotation.getSource();
		return source != null && source.startsWith(ASSISTANT_SOURCE_PREFIX);
	}

	/**
	 * @return true if an assistant wrote the annotation: it has an {@code ASSISTANT:} source, or
	 *         it has no source and was created by the assistant user (the old lexical path, which
	 *         wrote no source).
	 */
	public static boolean isMachineGenerated(Annotation annotation) {
		if (annotation == null) {
			return false;
		}
		if (isAssistantSourced(annotation)) {
			return true;
		}
		return annotation.getSource() == null && User.isAssistant(annotation.getCreatedBy());
	}

	/**
	 * Human discussion, which an assistant must never delete: the issue is resolved, or one of
	 * its positions, or an argument on one, was written by someone other than the assistant
	 * user. A position or argument with no author counts as human. Positions the assistant
	 * suggested (change spelling, add word, ignore, add to glossary, add actor) do not count. A
	 * note has no positions, so it never carries discussion.
	 */
	public static boolean hasHumanDiscussion(Annotation annotation) {
		if (!(annotation instanceof Issue issue)) {
			return false;
		}
		if (issue.isResolved()) {
			return true;
		}
		if (issue.getPositions() == null) {
			return false;
		}
		for (Position position : issue.getPositions()) {
			if (!User.isAssistant(position.getCreatedBy())) {
				return true;
			}
			if (position.getArguments() == null) {
				continue;
			}
			for (Argument argument : position.getArguments()) {
				if (!User.isAssistant(argument.getCreatedBy())) {
					return true;
				}
			}
		}
		return false;
	}
}
