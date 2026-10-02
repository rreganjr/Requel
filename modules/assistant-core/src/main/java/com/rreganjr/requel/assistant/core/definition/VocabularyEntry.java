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
package com.rreganjr.requel.assistant.core.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Issue #260: one finding type a definition may report, with what it means.
 *
 * @param type the {@code findingType} value, e.g. {@code AMBIGUOUS}
 * @param description one line saying when the type applies
 * @param category #263: {@link #QUALITY} (the default) or {@link #EXTRACTION} - the content is
 *        fine but belongs in another entity type, so the finding is advisory
 */
public record VocabularyEntry(String type, String description, String category) {

	public static final String QUALITY = "quality";
	public static final String EXTRACTION = "extraction";

	public VocabularyEntry {
		category = category == null || category.isBlank() ? QUALITY : category;
	}

	public VocabularyEntry(String type, String description) {
		this(type, description, QUALITY);
	}

	@JsonIgnore
	public boolean isExtraction() {
		return EXTRACTION.equals(category);
	}
}
