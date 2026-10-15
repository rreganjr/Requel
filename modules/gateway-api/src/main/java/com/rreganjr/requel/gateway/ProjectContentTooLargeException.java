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
package com.rreganjr.requel.gateway;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Issue #274: a project content read refused because the project's content is over the configured
 * character cap. The read fails rather than truncating, and the message names the overflow: the
 * total, the cap, and each section's entity count and characters.
 * <p>
 * Over REST it is a 422 with the code {@link #CODE}; {@code RestQueryGateway} turns that back into
 * this exception with the server's message.
 */
public class ProjectContentTooLargeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** The {@code ErrorResponse} code the REST read answers with. */
	public static final String CODE = "CONTENT_TOO_LARGE";

	/** One section's size: how many entities (or annotations) and how many characters. */
	public record SectionSize(int count, int characters) {
	}

	private final int characters;
	private final int maxCharacters;
	private final Map<String, SectionSize> sections;

	public ProjectContentTooLargeException(String projectName, int characters, int maxCharacters,
			Map<String, SectionSize> sections) {
		super(message(projectName, characters, maxCharacters, sections));
		this.characters = characters;
		this.maxCharacters = maxCharacters;
		this.sections = Collections.unmodifiableMap(new LinkedHashMap<>(sections));
	}

	/** The client side: the server's message, without the numbers behind it. */
	public ProjectContentTooLargeException(String message) {
		super(message);
		this.characters = -1;
		this.maxCharacters = -1;
		this.sections = Map.of();
	}

	/** The characters counted, or -1 when rebuilt from a REST response. */
	public int getCharacters() {
		return characters;
	}

	/** The cap, or -1 when rebuilt from a REST response. */
	public int getMaxCharacters() {
		return maxCharacters;
	}

	/** Each section's size, in read order; empty when rebuilt from a REST response. */
	public Map<String, SectionSize> getSections() {
		return sections;
	}

	private static String message(String projectName, int characters, int maxCharacters,
			Map<String, SectionSize> sections) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("Project '%s' content is %,d characters; the cap is %,d"
				+ " (requel.gateway.content.max-characters).", projectName, characters,
				maxCharacters));
		String separator = " ";
		for (Map.Entry<String, SectionSize> section : sections.entrySet()) {
			sb.append(separator).append(String.format("%s %,d / %,d", section.getKey(),
					section.getValue().count(), section.getValue().characters()));
			separator = "; ";
		}
		sb.append(". Retry with annotations=open or annotations=none, or read entities one at a"
				+ " time with getEntity.");
		return sb.toString();
	}
}
