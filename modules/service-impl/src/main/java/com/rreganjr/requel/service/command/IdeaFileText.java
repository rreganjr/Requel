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
package com.rreganjr.requel.service.command;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.web.multipart.MultipartFile;

/**
 * Reads the text of a file attached to an idea. One place for the rules, used by the
 * {@code SubmitIdea} command and by the new-project-from-idea endpoint.
 */
public final class IdeaFileText {

	/** The largest file accepted. */
	public static final long MAX_BYTES = 10L * 1024 * 1024;

	private IdeaFileText() {
	}

	/**
	 * @return the file's text, decoded as UTF-8
	 * @throws IllegalArgumentException when the file is not a .txt or .md file, or is too large
	 */
	public static String read(MultipartFile file) {
		String name = file.getOriginalFilename();
		String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
		if (!lower.endsWith(".txt") && !lower.endsWith(".md")) {
			throw new IllegalArgumentException(
					"Only .txt and .md files can be attached to an idea for now.");
		}
		if (file.getSize() > MAX_BYTES) {
			throw new IllegalArgumentException("The file is larger than 10 MB.");
		}
		try {
			return new String(file.getBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read uploaded file", e);
		}
	}
}
