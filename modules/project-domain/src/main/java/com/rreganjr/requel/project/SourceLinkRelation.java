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
package com.rreganjr.requel.project;

/**
 * Issues #272 and #273: how an entity relates to an {@link ExternalSource}. One record serves
 * both: where an entity came from, and which documents it refers to.
 */
public enum SourceLinkRelation {

	/** The entity was built from (a fragment of) the source. */
	DERIVED_FROM,

	/**
	 * Issue #273: the entity refers to the source (or one section of it); nothing was built from
	 * it. A citation carries no hashes or fingerprint and takes no part in an ingest decision.
	 */
	CITES;

	/**
	 * @return the relation named {@code value}, case-insensitively; {@link #DERIVED_FROM} for null
	 *         or blank, which is what every link was before #273.
	 * @throws IllegalArgumentException for any other value
	 */
	public static SourceLinkRelation parse(String value) {
		if (value == null || value.isBlank()) {
			return DERIVED_FROM;
		}
		String wanted = value.strip();
		for (SourceLinkRelation relation : values()) {
			if (relation.name().equalsIgnoreCase(wanted)) {
				return relation;
			}
		}
		throw new IllegalArgumentException("relation must be one of DERIVED_FROM, CITES");
	}
}
