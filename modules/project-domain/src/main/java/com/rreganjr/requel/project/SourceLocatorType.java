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
 * Issue #272: how an {@link ExternalSource}'s {@code locator} is to be read. Requel never opens
 * a locator; it is stored for people and for emit, and never reaches a model.
 */
public enum SourceLocatorType {

	/** An {@code http} or {@code https} URL. */
	URL,

	/**
	 * A relative path to a file, e.g. {@code docs/Roundtable-Production-Guide.pdf}: no scheme, not
	 * absolute, no {@code ..} segment.
	 */
	PATH;

	/**
	 * @return the type named by {@code value}, compared case-insensitively.
	 * @throws IllegalArgumentException for an unknown name
	 */
	public static SourceLocatorType parse(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("locatorType is required when a locator is given");
		}
		for (SourceLocatorType type : values()) {
			if (type.name().equalsIgnoreCase(value.strip())) {
				return type;
			}
		}
		throw new IllegalArgumentException(
				"unknown locatorType '" + value + "'; expected one of URL, PATH");
	}
}
