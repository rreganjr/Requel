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
package com.rreganjr.requel.tagging;

/**
 * The by-name string form of a tag used in the project XML round-trip (see
 * {@code doc/112-phase5-jaxb-tags.md}):
 *
 * <pre>
 *   token   := [ category ":" ] value [ "[" color "]" ]
 * </pre>
 *
 * {@code category} and {@code value} are {@link TagNormalizer} slugs ({@code [a-z0-9-]}), so the
 * {@code :} and {@code [} delimiters can never occur inside them — parsing is unambiguous. Examples:
 * {@code billing}, {@code type:business-rule}, {@code projectKind:product[#1d4ed8]}.
 *
 * @param category the namespace/dimension, or {@code null} for a flat tag
 * @param value the label (required)
 * @param color optional UI colour hint, or {@code null}
 * @author ron
 */
public record TagToken(String category, String value, String color) {

	/**
	 * Render this tag as its token string.
	 */
	public String toToken() {
		StringBuilder sb = new StringBuilder();
		if ((category != null) && !category.isEmpty()) {
			sb.append(category).append(':');
		}
		sb.append(value);
		if ((color != null) && !color.isEmpty()) {
			sb.append('[').append(color).append(']');
		}
		return sb.toString();
	}

	/**
	 * Parse a token string. Returns {@code null} if the token has no usable value.
	 */
	public static TagToken parse(String raw) {
		if (raw == null) {
			return null;
		}
		String rest = raw.trim();
		if (rest.isEmpty()) {
			return null;
		}
		String color = null;
		if (rest.endsWith("]")) {
			int open = rest.lastIndexOf('[');
			if (open >= 0) {
				color = rest.substring(open + 1, rest.length() - 1);
				rest = rest.substring(0, open);
			}
		}
		String category = null;
		String value = rest;
		int colon = rest.indexOf(':');
		if (colon >= 0) {
			category = rest.substring(0, colon);
			value = rest.substring(colon + 1);
		}
		if (value.isEmpty()) {
			return null;
		}
		return new TagToken(
				(category != null) && category.isEmpty() ? null : category,
				value,
				(color != null) && color.isEmpty() ? null : color);
	}
}
