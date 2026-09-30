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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.rreganjr.requel.assistant.ai.AiAnalysisException;
import com.rreganjr.requel.assistant.ai.AiAnalysisRequest;
import com.rreganjr.requel.assistant.ai.AiAnalysisResponse;
import com.rreganjr.requel.assistant.ai.AiProperties;
import com.rreganjr.requel.assistant.api.EntityRef;

/**
 * {@link CliAiAnalysisClient} over a fake {@link CliProcessRunner}: argv/stdin/environment
 * construction, both output formats, and every failure mode (#259). No CLI binary is involved.
 */
class CliAiAnalysisClientTest {

	private static final String HOSTILE = "$(touch /tmp/pwned) `id` '\"; | & > < \\ \n"
			+ "naïve — 日本語 ${HOME}";

	private static final String REPLY = "{\"summary\":\"one finding\",\"findings\":[{"
			+ "\"findingType\":\"AMBIGUOUS\",\"severity\":\"HIGH\",\"confidence\":0.7,"
			+ "\"evidenceReferences\":[\"fast\"],\"suggestedIssueText\":\"'fast' is vague\","
			+ "\"suggestedNoteText\":null,\"suggestedPositions\":[\"Set a latency budget\"]}]}";

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final FakeRunner runner = new FakeRunner();
	private final CliAiProperties cli = cliProperties();
	private final AiProperties ai = aiProperties();
	private Map<String, String> parentEnv = Map.of("PATH", "/usr/bin:/bin", "HOME", "/Users/dev",
			"USER", "dev", "LANG", "en_US.UTF-8", "GH_TOKEN", "secret", "AWS_PROFILE", "prod",
			"CLAUDE_CONFIG_DIR", "/Users/dev/.claude");

	private CliAiAnalysisClient client() {
		return new CliAiAnalysisClient(ai, cli, objectMapper, runner, () -> parentEnv,
				Clock.systemUTC());
	}

	private static CliAiProperties cliProperties() {
		CliAiProperties p = new CliAiProperties();
		p.setCommand("/usr/local/bin/claude");
		p.setArgs(List.of("-p", "--output-format", "json", "--tools", "", "--strict-mcp-config"));
		return p;
	}

	private static AiProperties aiProperties() {
		AiProperties p = new AiProperties();
		p.setEnabled(true);
		p.setProvider("cli");
		p.setModel("claude-cli");
		return p;
	}

	private AiAnalysisRequest request() {
		return new AiAnalysisRequest("ai-requirements-review", UUID.randomUUID(),
				"REQUIREMENTS_REVIEW", EntityRef.of("Goal", 10L), EntityRef.of("Project", 2L),
				Locale.US, List.of(Map.of("name", "Speed", "text", HOSTILE)),
				"RequirementsReviewOutput", "1",
				objectMapper.createObjectNode().put("type", "object"), Map.of(), Map.of(),
				"Review the goal.");
	}

	private static String envelope(String result) throws Exception {
		return new ObjectMapper().writeValueAsString(Map.of("type", "result", "is_error", false,
				"result", result, "total_cost_usd", 0.0123,
				"usage", Map.of("input_tokens", 2, "cache_creation_input_tokens", 1198,
						"output_tokens", 340, "cache_read_input_tokens", 800)));
	}

	// ---- invocation ------------------------------------------------------------

	@Test
	void argvIsTheCommandAndConfiguredArgsOnly() throws Exception {
		runner.stdout = envelope(REPLY);

		client().analyze(request());

		assertThat(runner.last.argv()).containsExactly("/usr/local/bin/claude", "-p",
				"--output-format", "json", "--tools", "", "--strict-mcp-config");
		assertThat(runner.last.argv()).noneMatch(arg -> arg.contains("touch")
				|| arg.contains("Speed") || arg.contains("Review the goal"));
	}

	@Test
	void hostileProjectTextArrivesUnchangedOnStdin() throws Exception {
		runner.stdout = envelope(REPLY);

		client().analyze(request());

		String stdin = new String(runner.last.stdin(), StandardCharsets.UTF_8);
		// The JSON body escapes the newline and quote; decode it back to compare exactly.
		String body = stdin.substring(stdin.indexOf("Request:\n") + "Request:\n".length(),
				stdin.indexOf("\n\n" + CliAiAnalysisClient.SCHEMA_INSTRUCTION));
		String packText = objectMapper.readTree(body).path("contextPacks").get(0).path("text")
				.asText();
		assertThat(packText).isEqualTo(HOSTILE);
		assertThat(stdin).startsWith("Review the goal.")
				.contains(CliAiAnalysisClient.SCHEMA_INSTRUCTION)
				.endsWith("{\n  \"type\" : \"object\"\n}\n");
	}

	@Test
	void withoutASchemaTheReplyShapeIsSpelledOut() throws Exception {
		runner.stdout = envelope(REPLY);
		AiAnalysisRequest noSchema = new AiAnalysisRequest("a", UUID.randomUUID(), "T",
				EntityRef.of("Goal", 1L), EntityRef.of("Project", 2L), Locale.US, List.of(), "n",
				"1", NullNode.getInstance(), Map.of(), Map.of());

		client().analyze(noSchema);

		assertThat(new String(runner.last.stdin(), StandardCharsets.UTF_8))
				.contains(CliAiAnalysisClient.SHAPE_INSTRUCTION)
				.doesNotContain(CliAiAnalysisClient.SCHEMA_INSTRUCTION);
	}

	@Test
	void theChildGetsOnlyTheAllowlistedEnvironment() throws Exception {
		runner.stdout = envelope(REPLY);
		cli.setEnv(List.of("CLAUDE_CONFIG_DIR", "NOT_SET_ANYWHERE"));

		client().analyze(request());

		assertThat(runner.last.environment()).containsOnly(
				Map.entry("PATH", "/usr/bin:/bin"), Map.entry("HOME", "/Users/dev"),
				Map.entry("USER", "dev"), Map.entry("LANG", "en_US.UTF-8"),
				Map.entry("CLAUDE_CONFIG_DIR", "/Users/dev/.claude"));
	}

	@Test
	void theWorkingDirectoryIsAFreshEmptyTempDirRemovedAfterwards() throws Exception {
		runner.stdout = envelope(REPLY);
		runner.onRun = invocation -> {
			assertThat(invocation.workingDirectory()).isEmptyDirectory();
			assertThat(invocation.workingDirectory().getFileName().toString())
					.startsWith("requel-ai-cli-");
			Files.writeString(invocation.workingDirectory().resolve("scratch.txt"), "x");
		};

		client().analyze(request());

		assertThat(runner.last.workingDirectory()).doesNotExist();
	}

	@Test
	void theWorkingDirectoryIsRemovedWhenTheRunFails() {
		runner.exitCode = 1;

		assertThatThrownBy(() -> client().analyze(request()))
				.isInstanceOf(AiAnalysisException.class);
		assertThat(runner.last.workingDirectory()).doesNotExist();
	}

	@Test
	void limitsComeFromConfiguration() throws Exception {
		runner.stdout = envelope(REPLY);
		cli.setTimeout(Duration.ofSeconds(42));
		cli.setMaxOutputBytes(1234);
		cli.setMaxErrorBytes(99);

		client().analyze(request());

		assertThat(runner.last.timeout()).isEqualTo(Duration.ofSeconds(42));
		assertThat(runner.last.maxOutputBytes()).isEqualTo(1234);
		assertThat(runner.last.maxErrorBytes()).isEqualTo(99);
	}

	// ---- output formats ----------------------------------------------------------

	@Test
	void claudeJsonEnvelopeIsDecodedWithUsageAndCost() throws Exception {
		runner.stdout = envelope("```json\n" + REPLY + "\n```");

		AiAnalysisResponse response = client().analyze(request());

		assertThat(response.summary()).isEqualTo("one finding");
		assertThat(response.findings()).singleElement().satisfies(f -> {
			assertThat(f.findingType()).isEqualTo("AMBIGUOUS");
			assertThat(f.suggestedPositions()).containsExactly("Set a latency budget");
		});
		assertThat(response.usage().provider()).isEqualTo("cli");
		assertThat(response.usage().model()).isEqualTo("claude-cli");
		// input = uncached (2) + cache creation (1198) + cache read (800)
		assertThat(response.usage().inputTokens()).isEqualTo(2000);
		assertThat(response.usage().outputTokens()).isEqualTo(340);
		assertThat(response.usage().cachedInputTokens()).isEqualTo(800);
		assertThat(response.usage().costEstimate()).isEqualByComparingTo(new BigDecimal("0.0123"));
		assertThat(response.providerMetadata()).containsEntry("provider", "cli")
				.containsEntry("outputFormat", "CLAUDE_JSON").containsEntry("exitCode", 0);
	}

	@Test
	void rawOutputIsTheReplyWithNoUsage() throws Exception {
		cli.setOutputFormat(CliAiProperties.OutputFormat.RAW);
		runner.stdout = REPLY + "\n";

		AiAnalysisResponse response = client().analyze(request());

		assertThat(response.summary()).isEqualTo("one finding");
		assertThat(response.usage().inputTokens()).isNull();
		assertThat(response.usage().costEstimate()).isNull();
		assertThat(response.usage().latency()).isNotNull();
	}

	@Test
	void anEnvelopeReportingAnErrorFails() throws Exception {
		runner.stdout = objectMapper.writeValueAsString(
				Map.of("type", "result", "is_error", true, "result", "Credit balance is too low"));

		assertThatThrownBy(() -> client().analyze(request()))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("reported an error")
				.hasMessageContaining("Credit balance");
	}

	@Test
	void anEnvelopeWithoutResultTextFails() {
		runner.stdout = "{\"type\":\"result\",\"is_error\":false}";

		assertThatThrownBy(() -> client().analyze(request()))
				.isInstanceOf(AiAnalysisException.class).hasMessageContaining("no result text");
	}

	// ---- failures ------------------------------------------------------------------

	@Test
	void aNonZeroExitFailsWithTheStderrExcerpt() {
		runner.exitCode = 2;
		runner.stderr = "Error: not logged in";

		assertThatThrownBy(() -> client().analyze(request()))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("exited with status 2")
				.hasMessageContaining("not logged in")
				.hasMessageNotContaining("touch /tmp/pwned");
	}

	@Test
	void aNonZeroExitWithAClaudeErrorEnvelopeCarriesItsReason() throws Exception {
		// claude -p --output-format json reports its own failures on stdout, stderr empty.
		runner.exitCode = 1;
		runner.stdout = objectMapper.writeValueAsString(Map.of("type", "result",
				"subtype", "error_during_execution", "is_error", true,
				"result", "Invalid API key · Please run /login"));

		assertThatThrownBy(() -> client().analyze(request()))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("exited with status 1")
				.hasMessageContaining("claude reported: error_during_execution")
				.hasMessageContaining("Please run /login");
	}

	@Test
	void aNonZeroExitWithOnlyStdoutCarriesAnExcerpt() {
		cli.setOutputFormat(CliAiProperties.OutputFormat.RAW);
		runner.exitCode = 1;
		runner.stdout = "Not logged in. Run codex login.";

		assertThatThrownBy(() -> client().analyze(request()))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("stdout: Not logged in");
	}

	@Test
	void aTimeoutFails() {
		runner.timedOut = true;
		runner.exitCode = -1;

		assertThatThrownBy(() -> client().analyze(request()))
				.isInstanceOf(AiAnalysisException.class).hasMessageContaining("timed out");
	}

	@Test
	void truncatedOutputIsRejectedRatherThanParsed() throws Exception {
		runner.stdout = envelope(REPLY);
		runner.truncated = true;

		assertThatThrownBy(() -> client().analyze(request()))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("max-output-bytes");
	}

	@Test
	void emptyOutputFails() {
		runner.stdout = "";

		assertThatThrownBy(() -> client().analyze(request()))
				.isInstanceOf(AiAnalysisException.class).hasMessageContaining("no output");
	}

	@Test
	void nonJsonOutputFails() {
		runner.stdout = "I reviewed the goal and it looks fine!";

		assertThatThrownBy(() -> client().analyze(request()))
				.isInstanceOf(AiAnalysisException.class).hasMessageContaining("claude-json");
	}

	@Test
	void aReplyThatIsNotTheContractFails() throws Exception {
		runner.stdout = envelope("{\"findings\":[]}"); // no summary

		assertThatThrownBy(() -> client().analyze(request()))
				.isInstanceOf(AiAnalysisException.class).hasMessageContaining("summary");
	}

	@Test
	void aProcessThatCannotStartFails() {
		runner.startFailure = new IOException("No such file or directory");

		assertThatThrownBy(() -> client().analyze(request()))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("/usr/local/bin/claude")
				.hasMessageContaining("No such file");
	}

	// ---- fake ------------------------------------------------------------------------

	@FunctionalInterface
	private interface OnRun {
		void accept(CliInvocation invocation) throws Exception;
	}

	private static final class FakeRunner implements CliProcessRunner {
		CliInvocation last;
		String stdout = "";
		String stderr = "";
		int exitCode;
		boolean timedOut;
		boolean truncated;
		IOException startFailure;
		OnRun onRun = invocation -> { };

		@Override
		public CliProcessResult run(CliInvocation invocation) throws IOException {
			last = invocation;
			if (startFailure != null) {
				throw startFailure;
			}
			try {
				onRun.accept(invocation);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			return new CliProcessResult(exitCode, stdout.getBytes(StandardCharsets.UTF_8),
					truncated, stderr, timedOut, Duration.ofMillis(5));
		}
	}

}
