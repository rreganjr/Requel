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
package com.rreganjr.requel.assistant.ai.cli;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One CLI execution: an argv list (never a shell string), the child's complete environment, its
 * working directory, the bytes to write to its stdin, and the limits it runs under.
 */
public record CliInvocation(List<String> argv, Map<String, String> environment,
		Path workingDirectory, byte[] stdin, Duration timeout, int maxOutputBytes,
		int maxErrorBytes) {

	public CliInvocation {
		argv = List.copyOf(Objects.requireNonNull(argv, "argv"));
		if (argv.isEmpty()) {
			throw new IllegalArgumentException("argv must name a command");
		}
		environment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
		Objects.requireNonNull(workingDirectory, "workingDirectory");
		Objects.requireNonNull(stdin, "stdin");
		Objects.requireNonNull(timeout, "timeout");
		if (maxOutputBytes <= 0 || maxErrorBytes <= 0) {
			throw new IllegalArgumentException("byte caps must be positive");
		}
	}
}
