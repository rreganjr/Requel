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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Size limits applied while building context packs. Defaults are tuned for
 * the first AI task ({@code REQUIREMENTS_REVIEW}) on a typical project; tune
 * via {@code requel.assistant.context-pack.*} properties.
 *
 * <p>Limits are best-effort: individual text fields are clamped at
 * {@code maxTextCharsPerField}, list-typed collections (annotations,
 * relations) are clamped at their respective per-entity caps, and the total
 * built pack is rejected if it exceeds {@code maxTotalCharacters}. The
 * builders never throw on oversize input — they truncate and record what was
 * clamped in {@link ContextPackMetadata}.</p>
 */
@ConfigurationProperties(prefix = "requel.assistant.context-pack")
public class ContextPackSizeLimits {

	/** Per-field text cap (description / text / annotation body). */
	private int maxTextCharsPerField = 4000;

	/** Maximum annotations carried per entity in EntityContextPack. */
	private int maxAnnotationsPerEntity = 50;

	/** Maximum issues surfaced project-wide in IssueContextPack. */
	private int maxProjectOpenIssues = 100;

	/** Hard total-character ceiling for a single built pack. */
	private int maxTotalCharacters = 100_000;

	public int getMaxTextCharsPerField() {
		return maxTextCharsPerField;
	}

	public void setMaxTextCharsPerField(int maxTextCharsPerField) {
		this.maxTextCharsPerField = maxTextCharsPerField;
	}

	public int getMaxAnnotationsPerEntity() {
		return maxAnnotationsPerEntity;
	}

	public void setMaxAnnotationsPerEntity(int maxAnnotationsPerEntity) {
		this.maxAnnotationsPerEntity = maxAnnotationsPerEntity;
	}

	public int getMaxProjectOpenIssues() {
		return maxProjectOpenIssues;
	}

	public void setMaxProjectOpenIssues(int maxProjectOpenIssues) {
		this.maxProjectOpenIssues = maxProjectOpenIssues;
	}

	public int getMaxTotalCharacters() {
		return maxTotalCharacters;
	}

	public void setMaxTotalCharacters(int maxTotalCharacters) {
		this.maxTotalCharacters = maxTotalCharacters;
	}
}
