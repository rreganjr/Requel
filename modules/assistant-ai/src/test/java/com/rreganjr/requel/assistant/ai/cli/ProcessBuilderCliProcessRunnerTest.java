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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * The real process boundary, exercised with ordinary OS binaries ({@code /bin/cat},
 * {@code /bin/sh}, {@code sleep}) — never an AI CLI (#259).
 */
@EnabledOnOs({ OS.LINUX, OS.MAC })
class ProcessBuilderCliProcessRunnerTest {

	private static final Map<String, String> ENV = Map.of("PATH", "/usr/bin:/bin");

	private final ProcessBuilderCliProcessRunner runner = new ProcessBuilderCliProcessRunner();

	@TempDir
	Path dir;

	private CliInvocation invocation(List<String> argv, byte[] stdin, Duration timeout,
			int maxOut) {
		return new CliInvocation(argv, ENV, dir, stdin, timeout, maxOut, 64 * 1024);
	}

	@Test
	void stdinRoundTripsThroughCat() throws Exception {
		byte[] input = new byte[1024 * 1024];
		Arrays.fill(input, (byte) 'x');
		input[0] = '$';

		CliProcessResult result = runner.run(invocation(List.of("/bin/cat"), input,
				Duration.ofSeconds(20), 2 * 1024 * 1024));

		assertThat(result.exitCode()).isZero();
		assertThat(result.timedOut()).isFalse();
		assertThat(result.stdoutTruncated()).isFalse();
		assertThat(result.stdout()).isEqualTo(input);
	}

	@Test
	void theExitCodeIsReported() throws Exception {
		CliProcessResult result = runner.run(invocation(List.of("/bin/sh", "-c", "exit 3"),
				new byte[0], Duration.ofSeconds(10), 1024));

		assertThat(result.exitCode()).isEqualTo(3);
		assertThat(result.timedOut()).isFalse();
	}

	@Test
	void theEnvironmentIsReplacedNotMerged() throws Exception {
		CliProcessResult result = runner.run(invocation(List.of("/usr/bin/env"), new byte[0],
				Duration.ofSeconds(10), 64 * 1024));

		String env = new String(result.stdout(), StandardCharsets.UTF_8).strip();
		assertThat(env.lines()).containsExactly("PATH=/usr/bin:/bin");
	}

	@Test
	void theWorkingDirectoryIsTheInvocations() throws Exception {
		CliProcessResult result = runner.run(invocation(List.of("/bin/pwd"), new byte[0],
				Duration.ofSeconds(10), 4096));

		assertThat(Path.of(new String(result.stdout(), StandardCharsets.UTF_8).strip())
				.toRealPath()).isEqualTo(dir.toRealPath());
	}

	@Test
	void aHungProcessIsKilledAtTheTimeout() throws Exception {
		long started = System.nanoTime();

		CliProcessResult result = runner.run(invocation(List.of("/bin/sh", "-c", "sleep 30"),
				new byte[0], Duration.ofMillis(300), 1024));

		assertThat(result.timedOut()).isTrue();
		assertThat(result.exitCode()).isEqualTo(-1);
		assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(15));
	}

	@Test
	void oversizeStdoutIsTruncatedWithoutHanging() throws Exception {
		CliProcessResult result = runner.run(invocation(
				List.of("/bin/sh", "-c", "head -c 5000000 /dev/zero"), new byte[0],
				Duration.ofSeconds(20), 1024));

		assertThat(result.exitCode()).isZero();
		assertThat(result.stdoutTruncated()).isTrue();
		assertThat(result.stdout()).hasSize(1024);
	}

	@Test
	void aStderrFloodDoesNotBlockTheChild() throws Exception {
		CliProcessResult result = runner.run(invocation(
				List.of("/bin/sh", "-c", "head -c 5000000 /dev/zero 1>&2; echo done"), new byte[0],
				Duration.ofSeconds(20), 1024));

		assertThat(result.timedOut()).isFalse();
		assertThat(result.exitCode()).isZero();
		assertThat(new String(result.stdout(), StandardCharsets.UTF_8).strip()).isEqualTo("done");
		assertThat(result.stderr().length()).isLessThanOrEqualTo(64 * 1024);
	}

	@Test
	void aChildThatIgnoresStdinDoesNotFailTheRun() throws Exception {
		byte[] input = new byte[4 * 1024 * 1024];

		CliProcessResult result = runner.run(invocation(List.of("/bin/sh", "-c", "echo hi"),
				input, Duration.ofSeconds(20), 1024));

		assertThat(result.exitCode()).isZero();
		assertThat(new String(result.stdout(), StandardCharsets.UTF_8).strip()).isEqualTo("hi");
	}
}
