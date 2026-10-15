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
package com.rreganjr.requel.service.command;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.requel.project.SwitchableAssistantCatalog;
import com.rreganjr.requel.project.SwitchableAssistantCatalog.SwitchableAssistant;

/**
 * Issue #265: what raised an annotation, for the UI's "Policy: ..." tag. An annotation's
 * {@code source} is {@code ASSISTANT:<id>}; the id is looked up among the switchable assistants
 * (bean assistants and bundled definitions) for its display name and group. The DTO mappers are
 * static, so the lookup is too: this bean installs the catalog at startup, and without it (unit
 * tests) both answers are null.
 */
@Component
public class AnnotationSources {

	static final String ASSISTANT_PREFIX = "ASSISTANT:";
	public static final String POLICY = "POLICY";
	public static final String REVIEW = "REVIEW";
	public static final String LEXICAL = "LEXICAL";
	/** #266: a relationship finding from a corpus run. */
	public static final String CORPUS = "CORPUS";

	private static volatile ObjectProvider<SwitchableAssistantCatalog> catalog;

	@Autowired
	public AnnotationSources(ObjectProvider<SwitchableAssistantCatalog> provider) {
		catalog = provider;
	}

	/** The display name of the assistant behind {@code source}, or null. */
	public static String name(String source) {
		return name(source, null);
	}

	/**
	 * Issue #264: as {@link #name(String)}, reading {@code projectId}'s own definitions too, so an
	 * issue a project's definition raised shows its name.
	 */
	public static String name(String source, Long projectId) {
		SwitchableAssistant assistant = lookup(source, projectId);
		return assistant == null ? null : assistant.displayName();
	}

	/** POLICY, REVIEW, CORPUS or LEXICAL for an assistant's {@code source}, or null. */
	public static String kind(String source) {
		return kind(source, null);
	}

	/** Issue #264: as {@link #kind(String)}, reading {@code projectId}'s own definitions too. */
	public static String kind(String source, Long projectId) {
		SwitchableAssistant assistant = lookup(source, projectId);
		if (assistant == null) {
			return null;
		}
		if (SwitchableAssistantCatalog.POLICIES.equals(assistant.group())) {
			return POLICY;
		}
		if (SwitchableAssistantCatalog.CORPUS.equals(assistant.group())) {
			return CORPUS;
		}
		return SwitchableAssistantCatalog.AI_REVIEW.equals(assistant.group()) ? REVIEW : LEXICAL;
	}

	private static SwitchableAssistant lookup(String source, Long projectId) {
		ObjectProvider<SwitchableAssistantCatalog> provider = catalog;
		if (source == null || !source.startsWith(ASSISTANT_PREFIX) || provider == null) {
			return null;
		}
		String id = source.substring(ASSISTANT_PREFIX.length());
		try {
			SwitchableAssistantCatalog found = provider.getIfAvailable();
			if (found == null) {
				return null;
			}
			return found.describe(id, projectId).orElse(null);
		} catch (RuntimeException e) {
			return null;
		}
	}
}
