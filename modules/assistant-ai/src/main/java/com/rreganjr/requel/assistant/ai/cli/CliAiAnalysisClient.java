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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.ai.AiAnalysisClient;
import com.rreganjr.requel.assistant.ai.AiAnalysisException;
import com.rreganjr.requel.assistant.ai.AiAnalysisRequest;
import com.rreganjr.requel.assistant.ai.AiAnalysisResponse;
import com.rreganjr.requel.assistant.ai.AiPromptBuilder;
import com.rreganjr.requel.assistant.ai.AiProperties;
import com.rreganjr.requel.assistant.ai.AiProviderLocality;
import com.rreganjr.requel.assistant.ai.DataHandlingGuard;
import com.rreganjr.requel.assistant.ai.AiUsage;
import com.rreganjr.requel.assistant.ai.ReviewResultMapper;
import com.rreganjr.requel.assistant.ai.ReviewResultMapper.ReviewResult;

/**
 * <strong>Development-only</strong> {@link AiAnalysisClient} that shells out to a locally
 * authenticated {@code claude} or {@code codex} CLI (#259). It exists so prompt work can move
 * forward on a developer's machine without an API key; it is not a deployment provider, does not
 * work in the Docker image, and must not serve other users.
 *
 * <p>
 * The prompt (shared task guidance and JSON body, plus the request's output schema, since there is
 * no structured-output converter here) is written to the child's <em>stdin</em>. The command is an
 * argv list from configuration; no prompt text ever reaches it. The child runs with an allowlisted
 * environment in a fresh empty temp directory, under a timeout and byte caps (see
 * {@link ProcessBuilderCliProcessRunner}). Tools and MCP servers are turned off by the configured
 * args. Every failure — start failure, non-zero exit, timeout, oversize, empty or unparseable output
 * — is an {@link AiAnalysisException}; there is no partial response.
 */
public class CliAiAnalysisClient implements AiAnalysisClient {

	private static final Logger log = LoggerFactory.getLogger(CliAiAnalysisClient.class);

	/** Environment variables every child gets when set; extras come from {@code requel.ai.cli.env}. */
	static final List<String> BASE_ENVIRONMENT = List.of("PATH", "HOME", "USER", "LANG", "TMPDIR");

	/** How much of stderr (or an error reply) an exception message carries. */
	static final int ERROR_EXCERPT_CHARS = 2048;

	static final String SCHEMA_INSTRUCTION = "Reply with a single JSON object matching this JSON "
			+ "Schema and nothing else: no prose before or after it and no code fence.";

	static final String SHAPE_INSTRUCTION = "Reply with a single JSON object with \"summary\" "
			+ "(string), \"findings\" (array) and \"warnings\" (array of strings), and nothing else.";

	private final AiProperties aiProperties;
	private final CliAiProperties cliProperties;
	private final ObjectMapper objectMapper;
	private final ReviewResultMapper mapper;
	private final AiPromptBuilder promptBuilder;
	private final CliProcessRunner runner;
	private final Supplier<Map<String, String>> parentEnvironment;
	private final Clock clock;

	public CliAiAnalysisClient(AiProperties aiProperties, CliAiProperties cliProperties,
			ObjectMapper objectMapper, CliProcessRunner runner,
			Supplier<Map<String, String>> parentEnvironment, Clock clock) {
		this.aiProperties = Objects.requireNonNull(aiProperties, "aiProperties");
		this.cliProperties = Objects.requireNonNull(cliProperties, "cliProperties");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.mapper = new ReviewResultMapper(objectMapper);
		this.promptBuilder = new AiPromptBuilder(objectMapper, aiProperties);
		this.runner = Objects.requireNonNull(runner, "runner");
		this.parentEnvironment = Objects.requireNonNull(parentEnvironment, "parentEnvironment");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	@Override
	public AiAnalysisResponse analyze(AiAnalysisRequest request) throws AiAnalysisException {
		Objects.requireNonNull(request, "request");
		// #262: the CLIs call their vendor's API, so this is always a remote provider
		DataHandlingGuard.requireAllowed(request, AiProviderLocality.REMOTE);
		Instant startedAt = clock.instant();
		byte[] stdin = prompt(request).getBytes(StandardCharsets.UTF_8);

		CliProcessResult result = execute(stdin);
		checkProcess(result);

		String stdout = new String(result.stdout(), StandardCharsets.UTF_8);
		Decoded decoded = switch (cliProperties.getOutputFormat()) {
			case CLAUDE_JSON -> decodeClaudeEnvelope(stdout);
			case RAW -> new Decoded(stdout, null, null, null, null);
		};

		ReviewResult review = mapper.parse(decoded.reply());
		ReviewResultMapper.validate(review);
		Duration latency = Duration.between(startedAt, clock.instant());
		AiUsage usage = new AiUsage(aiProperties.getProvider(), aiProperties.getModel(),
				decoded.inputTokens(), decoded.outputTokens(), decoded.cachedInputTokens(), latency,
				decoded.cost());
		if (log.isDebugEnabled()) {
			log.debug("cli provider reply for run {} ({} findings, {} ms)", request.runId(),
					review.findings() == null ? 0 : review.findings().size(), latency.toMillis());
		}
		return mapper.toResponse(review, usage, metadata(result));
	}

	// ---- prompt + invocation (package-visible for the argv/stdin tests) ------

	/** Task guidance, the JSON request body, then the output-format instruction and schema. */
	String prompt(AiAnalysisRequest request) {
		StringBuilder prompt = new StringBuilder()
				.append(promptBuilder.instructions(request))
				.append("\n\nRequest:\n")
				.append(promptBuilder.prompt(request))
				.append("\n\n");
		JsonNode schema = request.outputSchema();
		if (schema == null || schema.isNull() || schema.isMissingNode()) {
			prompt.append(SHAPE_INSTRUCTION).append('\n');
		} else {
			prompt.append(SCHEMA_INSTRUCTION).append('\n').append(pretty(schema)).append('\n');
		}
		return prompt.toString();
	}

	/** {@code command} followed by the configured {@code args}; nothing else. */
	List<String> argv() {
		List<String> argv = new ArrayList<String>();
		argv.add(cliProperties.getCommand());
		argv.addAll(cliProperties.getArgs());
		return argv;
	}

	/** The allowlisted subset of the JVM's environment: base names plus configured extras. */
	Map<String, String> environment() {
		Map<String, String> parent = parentEnvironment.get();
		Map<String, String> child = new LinkedHashMap<String, String>();
		List<String> names = new ArrayList<String>(BASE_ENVIRONMENT);
		names.addAll(cliProperties.getEnv());
		for (String name : names) {
			String value = parent.get(name);
			if (value != null) {
				child.put(name, value);
			}
		}
		return child;
	}

	private CliProcessResult execute(byte[] stdin) throws AiAnalysisException {
		Path workingDirectory = null;
		try {
			workingDirectory = Files.createTempDirectory("requel-ai-cli-");
			return runner.run(new CliInvocation(argv(), environment(), workingDirectory, stdin,
					cliProperties.getTimeout(), cliProperties.getMaxOutputBytes(),
					cliProperties.getMaxErrorBytes()));
		} catch (IOException e) {
			throw new AiAnalysisException("could not run the AI CLI " + cliProperties.getCommand()
					+ ": " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AiAnalysisException("interrupted while waiting for the AI CLI", e);
		} finally {
			deleteRecursively(workingDirectory);
		}
	}

	private void checkProcess(CliProcessResult result) throws AiAnalysisException {
		if (result.timedOut()) {
			throw new AiAnalysisException("AI CLI timed out after " + cliProperties.getTimeout()
					+ stderrSuffix(result));
		}
		if (result.exitCode() != 0) {
			throw new AiAnalysisException("AI CLI exited with status " + result.exitCode()
					+ failureDetail(result));
		}
		if (result.stdoutTruncated()) {
			throw new AiAnalysisException("AI CLI output exceeded requel.ai.cli.max-output-bytes="
					+ cliProperties.getMaxOutputBytes() + "; rejected rather than parsed");
		}
		if (result.stdout().length == 0) {
			throw new AiAnalysisException("AI CLI produced no output" + stderrSuffix(result));
		}
	}

	/**
	 * Read the {@code claude -p --output-format json} envelope: {@code is_error} fails the call,
	 * {@code result} is the reply, and {@code usage} / {@code total_cost_usd} feed {@link AiUsage}.
	 */
	private Decoded decodeClaudeEnvelope(String stdout) throws AiAnalysisException {
		JsonNode envelope;
		try {
			envelope = objectMapper.readTree(stdout);
		} catch (JsonProcessingException e) {
			throw new AiAnalysisException("AI CLI output is not a claude-json envelope: "
					+ e.getOriginalMessage(), e);
		}
		if (envelope == null || !envelope.isObject()) {
			throw new AiAnalysisException("AI CLI output is not a claude-json envelope");
		}
		JsonNode result = envelope.get("result");
		if (envelope.path("is_error").asBoolean(false)) {
			throw new AiAnalysisException("AI CLI reported an error"
					+ (result != null && result.isTextual() ? ": " + excerpt(result.asText()) : ""));
		}
		if (result == null || !result.isTextual()) {
			throw new AiAnalysisException("claude-json envelope has no result text");
		}
		JsonNode usage = envelope.path("usage");
		JsonNode cost = envelope.get("total_cost_usd");
		// claude's input_tokens counts only the uncached part (2 on a real run, next to 17,500
		// cache-creation tokens), so the recorded input is the sum of all three; the cache-read
		// share is also reported on its own.
		Integer cacheRead = intOrNull(usage.get("cache_read_input_tokens"));
		Integer input = sum(intOrNull(usage.get("input_tokens")),
				intOrNull(usage.get("cache_creation_input_tokens")), cacheRead);
		return new Decoded(result.asText(), input, intOrNull(usage.get("output_tokens")), cacheRead,
				cost != null && cost.isNumber() ? cost.decimalValue() : null);
	}

	private Map<String, Object> metadata(CliProcessResult result) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("provider", aiProperties.getProvider());
		metadata.put("model", aiProperties.getModel());
		metadata.put("outputFormat", cliProperties.getOutputFormat().name());
		metadata.put("exitCode", result.exitCode());
		return metadata;
	}

	private String pretty(JsonNode node) {
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
		} catch (JsonProcessingException e) {
			return node.toString();
		}
	}

	/**
	 * What a failed CLI said. {@code claude -p --output-format json} reports its own errors (auth,
	 * unknown option, max turns) as an {@code is_error} envelope on <em>stdout</em> with nothing on
	 * stderr, so stderr alone can leave the run with no reason at all. Order: the envelope's
	 * {@code subtype} and {@code result}, then stderr, then a stdout excerpt.
	 */
	private String failureDetail(CliProcessResult result) {
		StringBuilder detail = new StringBuilder();
		String stdout = new String(result.stdout(), StandardCharsets.UTF_8).strip();
		boolean described = false;
		if (!stdout.isEmpty() && cliProperties.getOutputFormat() == CliAiProperties.OutputFormat.CLAUDE_JSON) {
			try {
				JsonNode envelope = objectMapper.readTree(stdout);
				if (envelope != null && envelope.isObject()) {
					String subtype = envelope.path("subtype").asText("");
					JsonNode text = envelope.get("result");
					detail.append("; claude reported: ").append(subtype.isEmpty() ? "error" : subtype);
					if (text != null && text.isTextual() && !text.asText().isBlank()) {
						detail.append(": ").append(excerpt(text.asText().strip()));
					}
					described = true;
				}
			} catch (JsonProcessingException e) {
				// Not an envelope: fall through to the raw excerpt.
			}
		}
		detail.append(stderrSuffix(result));
		if (!described && result.stderr().isBlank() && !stdout.isEmpty()) {
			detail.append("; stdout: ").append(excerpt(stdout));
		}
		return detail.toString();
	}

	private static String stderrSuffix(CliProcessResult result) {
		String stderr = result.stderr().strip();
		return stderr.isEmpty() ? "" : "; stderr: " + excerpt(stderr);
	}

	private static String excerpt(String text) {
		return text.length() <= ERROR_EXCERPT_CHARS ? text
				: text.substring(0, ERROR_EXCERPT_CHARS) + "…";
	}

	/** Sum of the non-null parts; null when all are null. */
	private static Integer sum(Integer... parts) {
		Integer total = null;
		for (Integer part : parts) {
			if (part != null) {
				total = (total == null ? 0 : total) + part;
			}
		}
		return total;
	}

	private static Integer intOrNull(JsonNode node) {
		return node != null && node.isNumber() ? node.intValue() : null;
	}

	private static void deleteRecursively(Path directory) {
		if (directory == null) {
			return;
		}
		try (Stream<Path> paths = Files.walk(directory)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					log.debug("could not delete {}: {}", path, e.getMessage());
				}
			});
		} catch (IOException e) {
			log.debug("could not clean up {}: {}", directory, e.getMessage());
		}
	}

	/** The reply text plus whatever usage the output format reports. */
	private record Decoded(String reply, Integer inputTokens, Integer outputTokens,
			Integer cachedInputTokens, BigDecimal cost) {
	}
}
