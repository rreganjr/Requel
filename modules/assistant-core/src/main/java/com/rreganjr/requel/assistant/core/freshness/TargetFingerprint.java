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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.rreganjr.platform.domain.NamedEntity;
import com.rreganjr.requel.project.TextEntity;

/**
 * Issue #270: the fingerprint of the text an assistant finding was derived from. It is the
 * SHA-256 hex of the entity's name and text, each trimmed and with runs of whitespace collapsed
 * to one space (case is kept, a null reads as empty), so an edit that only reflows the text does
 * not make a finding stale, and a tag, relation or primary-actor change does not touch it.
 *
 * <p>
 * The one place the rule lives: the applicator writes it (as the run analyzed the target) and
 * {@link FindingFreshness} compares it with the entity as it is now.
 */
public final class TargetFingerprint {

	/** Separates name from text so ("ab", "c") and ("a", "bc") differ. */
	private static final char SEPARATOR = '\u0000';

	private TargetFingerprint() {
	}

	/**
	 * @return the fingerprint of {@code target}, or null for something with neither a name nor a
	 *         text (nothing to compare, so a finding on it never reads stale).
	 */
	public static String of(Object target) {
		// A Hibernate proxy subclasses the entity, so the instanceof checks and getters see it.
		Object entity = target;
		boolean named = entity instanceof NamedEntity;
		boolean texted = entity instanceof TextEntity;
		if (!named && !texted) {
			return null;
		}
		String name = named ? ((NamedEntity) entity).getName() : null;
		String text = texted ? ((TextEntity) entity).getText() : null;
		return of(name, text);
	}

	/** @return the fingerprint of a name and a text, either of which may be null. */
	public static String of(String name, String text) {
		String input = normalize(name) + SEPARATOR + normalize(text);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}

	static String normalize(String value) {
		return value == null ? "" : value.strip().replaceAll("\\s+", " ");
	}
}
