/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project;

import java.util.Date;

import com.rreganjr.platform.identity.User;

/**
 * Issue #320: an assistant finding a user chose to ignore on one entity and property, by
 * accepting the "Ignore" position on its issue. Assistants never raise a finding whose
 * idempotency key matches an ignored one (compared lower-case), so an ignore holds even after
 * the resolved issue is deleted.
 * <p>
 * The key is the assistant finding's idempotency key,
 * {@code <assistant>:<entity type>:<entity id>:<finding type>[:<property>]:<subject>}, kept in
 * parts so an export can rebuild it for the entity's new id on import.
 */
public interface IgnoredFinding {

	Long getId();

	Long getProjectId();

	/** The annotatable discriminator of the entity, e.g. {@code "Goal"}. */
	String getTargetType();

	Long getTargetId();

	String getAssistantId();

	/** e.g. {@code unknown-word}, {@code vague-word}, {@code complex-text}, {@code glossary-term}. */
	String getFindingType();

	/** The entity property the finding is about, or null (glossary terms). */
	String getPropertyName();

	/** The key after {@code <assistant>:<entity type>:<entity id>:}. */
	String getKeySuffix();

	/** The full idempotency key for the entity's current id. */
	default String getIdempotencyKey() {
		return idempotencyKey(getAssistantId(), getTargetType(), getTargetId(), getKeySuffix());
	}

	/** What was ignored, for display: the word, the phrase or a snippet of the sentence. */
	String getSubject();

	/** The resolved issue the ignore came from, when known. */
	Long getAnnotationId();

	User getCreatedBy();

	Date getDateCreated();

	/** {@code <assistant>:<entity type>:<entity id>:<suffix>}. */
	static String idempotencyKey(String assistantId, String targetType, Long targetId,
			String keySuffix) {
		return keyPrefix(assistantId, targetType, targetId) + keySuffix;
	}

	/** The part of an idempotency key that names the assistant and the entity. */
	static String keyPrefix(String assistantId, String targetType, Long targetId) {
		return assistantId + ":" + targetType + ":" + targetId + ":";
	}
}
