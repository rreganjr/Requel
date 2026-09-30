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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code requel.ai.cli.*} — the development-only CLI provider (#259). Bound only when
 * {@code requel.ai.provider=cli}; see {@code application-ai-cli.properties} for the invocations.
 */
@ConfigurationProperties(prefix = "requel.ai.cli")
public class CliAiProperties {

	/** How the CLI's stdout is read. */
	public enum OutputFormat {
		/** {@code claude -p --output-format json}: an envelope whose {@code result} is the reply. */
		CLAUDE_JSON,
		/** stdout is the model's reply (e.g. {@code codex exec}). */
		RAW
	}

	/** Absolute path to the CLI binary. No default: a real binary is never assumed. */
	private String command;
	/** argv after the command. Prompt text never appears here; it goes on stdin. */
	private List<String> args = new ArrayList<String>();
	private OutputFormat outputFormat = OutputFormat.CLAUDE_JSON;
	/** Extra environment variable names passed through to the child when set in the JVM. */
	private List<String> env = new ArrayList<String>();
	private Duration timeout = Duration.ofSeconds(180);
	private int maxOutputBytes = 1024 * 1024;
	private int maxErrorBytes = 64 * 1024;

	/**
	 * Fail fast on a CLI provider that was asked for but cannot run: blank, relative or
	 * non-executable {@code command}, or a non-positive limit.
	 *
	 * @throws IllegalStateException naming the offending property
	 */
	public void validate() {
		if (command == null || command.isBlank()) {
			throw new IllegalStateException("requel.ai.provider=cli requires requel.ai.cli.command "
					+ "(the absolute path to the claude or codex binary)");
		}
		Path path = Path.of(command);
		if (!path.isAbsolute()) {
			throw new IllegalStateException(
					"requel.ai.cli.command must be an absolute path: " + command);
		}
		if (!Files.isRegularFile(path) || !Files.isExecutable(path)) {
			throw new IllegalStateException(
					"requel.ai.cli.command is not an executable file: " + command);
		}
		if (timeout == null || timeout.isZero() || timeout.isNegative()) {
			throw new IllegalStateException("requel.ai.cli.timeout must be positive");
		}
		if (maxOutputBytes <= 0 || maxErrorBytes <= 0) {
			throw new IllegalStateException(
					"requel.ai.cli.max-output-bytes and max-error-bytes must be positive");
		}
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = new ArrayList<String>(Objects.requireNonNull(args, "args"));
	}

	public OutputFormat getOutputFormat() {
		return outputFormat;
	}

	public void setOutputFormat(OutputFormat outputFormat) {
		this.outputFormat = Objects.requireNonNull(outputFormat, "outputFormat");
	}

	public List<String> getEnv() {
		return env;
	}

	public void setEnv(List<String> env) {
		this.env = new ArrayList<String>(Objects.requireNonNull(env, "env"));
	}

	public Duration getTimeout() {
		return timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public int getMaxOutputBytes() {
		return maxOutputBytes;
	}

	public void setMaxOutputBytes(int maxOutputBytes) {
		this.maxOutputBytes = maxOutputBytes;
	}

	public int getMaxErrorBytes() {
		return maxErrorBytes;
	}

	public void setMaxErrorBytes(int maxErrorBytes) {
		this.maxErrorBytes = maxErrorBytes;
	}
}
