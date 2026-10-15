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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.rreganjr.requel.assistant.ai.AiAnalysisClient;
import com.rreganjr.requel.assistant.ai.AiConfiguration;
import com.rreganjr.requel.assistant.ai.NoopAiAnalysisClient;

/**
 * Provider selection and boot validation for the CLI client (#259): {@code provider=cli} yields
 * exactly one {@link AiAnalysisClient}, the CLI one; no provider yields only noop; and a cli
 * provider that cannot run fails the context naming the property.
 */
@EnabledOnOs({ OS.LINUX, OS.MAC })
class CliAiClientConfigurationTest {

	@TempDir
	Path dir;

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
			.withUserConfiguration(AiConfiguration.class, NoopAiAnalysisClient.class,
					CliAiClientConfiguration.class);

	private Path executable() throws Exception {
		Path script = dir.resolve("fake-cli");
		Files.writeString(script, "#!/bin/sh\necho '{}'\n");
		Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
		return script;
	}

	@Test
	void cliProviderRegistersOnlyTheCliClient() throws Exception {
		String command = executable().toString();
		runner.withPropertyValues("requel.ai.provider=cli", "requel.ai.cli.command=" + command,
				"requel.ai.cli.output-format=raw", "requel.ai.cli.args[0]=-p",
				"requel.ai.cli.args[1]=", "requel.ai.cli.args[2]=--strict-mcp-config")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).getBeans(AiAnalysisClient.class).hasSize(1);
					assertThat(context).hasSingleBean(CliAiAnalysisClient.class);
					CliAiProperties properties = context.getBean(CliAiProperties.class);
					assertThat(properties.getOutputFormat())
							.isEqualTo(CliAiProperties.OutputFormat.RAW);
					// An empty indexed element survives binding (the claude `--tools ""` case).
					assertThat(properties.getArgs()).containsExactly("-p", "",
							"--strict-mcp-config");
				});
	}

	@Test
	void noProviderRegistersOnlyNoop() {
		runner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).getBeans(AiAnalysisClient.class).hasSize(1);
			assertThat(context).hasSingleBean(NoopAiAnalysisClient.class);
			assertThat(context).doesNotHaveBean(CliAiAnalysisClient.class);
		});
	}

	@Test
	void aBlankCommandFailsTheContext() {
		runner.withPropertyValues("requel.ai.provider=cli").run(context -> assertThat(context)
				.hasFailed().getFailure().rootCause().hasMessageContaining("requel.ai.cli.command"));
	}

	@Test
	void aRelativeCommandFailsTheContext() {
		runner.withPropertyValues("requel.ai.provider=cli", "requel.ai.cli.command=claude")
				.run(context -> assertThat(context).hasFailed().getFailure().rootCause()
						.hasMessageContaining("absolute path"));
	}

	@Test
	void aNonExecutableCommandFailsTheContext() throws Exception {
		Path notExecutable = Files.writeString(dir.resolve("plain.txt"), "x");
		runner.withPropertyValues("requel.ai.provider=cli",
				"requel.ai.cli.command=" + notExecutable)
				.run(context -> assertThat(context).hasFailed().getFailure().rootCause()
						.hasMessageContaining("not an executable file"));
	}
}
