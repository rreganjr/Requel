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

package com.rreganjr.requel.project.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Issue #275: the report generators Requel bundles. A generator whose
 * {@link com.rreganjr.requel.project.ReportGenerator#getBuiltinKey() builtin key} names one of
 * these renders the bundled resource, not its stored copy, so an updated bundle reaches every
 * project. Every new project gets one of each.
 */
public final class BuiltinReportGenerators {

	/** One bundled generator: its key, the name a project shows, and its classpath resource. */
	public record Builtin(String key, String name, String resourcePath) {

		/** @return the bundled template text, UTF-8. */
		public String text() {
			ClassLoader loader = BuiltinReportGenerators.class.getClassLoader();
			try (InputStream in = loader.getResourceAsStream(resourcePath)) {
				if (in == null) {
					throw new IllegalStateException("bundled report generator " + key
							+ " is missing its resource " + resourcePath);
				}
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new IllegalStateException("bundled report generator " + key
						+ " could not be read from " + resourcePath, e);
			}
		}
	}

	public static final Builtin PROJECT_HTML = new Builtin("project-html", "HTML Specification",
			"xslt/project2html.xslt");
	public static final Builtin TICKET_MARKDOWN = new Builtin("ticket-markdown",
			"Ticket (Markdown)", "xslt/project2ticket-md.xslt");

	/** In creation order. */
	public static final List<Builtin> ALL = List.of(PROJECT_HTML, TICKET_MARKDOWN);

	private BuiltinReportGenerators() {
	}

	public static Optional<Builtin> forKey(String key) {
		if (key == null) {
			return Optional.empty();
		}
		return ALL.stream().filter(b -> b.key().equals(key)).findFirst();
	}
}
