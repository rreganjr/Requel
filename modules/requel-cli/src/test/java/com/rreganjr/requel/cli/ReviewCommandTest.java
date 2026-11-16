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
package com.rreganjr.requel.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.rreganjr.requel.gateway.AiReviewGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.service.api.dto.AiReviewDto;
import com.rreganjr.requel.service.api.dto.AiReviewRequestDto;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/** Issue #387: {@code requel review request|show}. */
class ReviewCommandTest {

    /** Hands out the queued latest-run answers in order; the last one repeats; none: never reviewed. */
    private static final class FakeGateway implements AiReviewGateway {
        final Deque<Optional<AiReviewDto>> latest = new ArrayDeque<>();
        GatewayException refusal;
        String lastTaskType = "unset";
        int reads;

        @Override
        public AiReviewRequestDto requestReview(String projectName, String entityType,
                long entityId) throws GatewayException {
            if (refusal != null) {
                throw refusal;
            }
            return new AiReviewRequestDto("run-1", null);
        }

        @Override
        public Optional<AiReviewDto> latestReview(String projectName, String entityType,
                long entityId, String taskType) {
            reads++;
            lastTaskType = taskType;
            if (latest.isEmpty()) {
                return Optional.empty();
            }
            return latest.size() > 1 ? latest.poll() : latest.peek();
        }
    }

    private final FakeGateway fake = new FakeGateway();
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void captureOutput() {
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private int run(String... args) {
        RequelCli root = new RequelCli();
        CommandLine cli = new CommandLine(root).setCaseInsensitiveEnumValuesAllowed(true);
        ReviewCommand review = (ReviewCommand) cli.getSubcommands().get("review").getCommand();
        review.gatewayOverride = fake;
        review.pollInterval = Duration.ofMillis(1);
        review.waitLimit = Duration.ofMillis(200);
        return cli.execute(args);
    }

    private static AiReviewDto run(String id, String status, List<AiReviewDto.Finding> findings) {
        return new AiReviewDto(id, status, Instant.parse("2026-10-30T12:00:00Z"), null, null, null,
                "FAILED".equals(status) ? "provider timed out" : null,
                "SUCCEEDED".equals(status) ? "Two problems." : null, 0, findings, 0, null, null, 0,
                null);
    }

    private static AiReviewDto.Finding finding(String kind, String severity, String text) {
        return new AiReviewDto.Finding("AMBIGUOUS", kind, severity, 0.9, text, "ACTIVE", 5L,
                "Goal", 7L);
    }

    @Test
    void requestPrintsTheQueuedRun() {
        assertThat(run("review", "request", "Demo", "Goal", "7")).isEqualTo(ExitCode.SUCCESS);
        assertThat(out.toString(StandardCharsets.UTF_8)).contains("Review queued: run run-1");
    }

    @Test
    void requestWaitFollowsTheRunToSucceededAndPrintsTheFindings() {
        fake.latest.add(Optional.of(run("old", "SUCCEEDED", List.of())));
        fake.latest.add(Optional.of(run("run-1", "QUEUED", List.of())));
        fake.latest.add(Optional.of(run("run-1", "RUNNING", List.of())));
        fake.latest.add(Optional.of(run("run-1", "SUCCEEDED", List.of(
                finding("ISSUE", "HIGH", "Fast is not measurable."),
                finding("NOTE", null, "Consider a target.")))));

        assertThat(run("review", "request", "Demo", "Goal", "7", "--wait"))
                .isEqualTo(ExitCode.SUCCESS);
        String text = out.toString(StandardCharsets.UTF_8);
        assertThat(text).contains("SUCCEEDED run run-1 (1 issue, 1 note)")
                .contains("Two problems.")
                .contains("HIGH   ISSUE Fast is not measurable.")
                .contains("       NOTE  Consider a target.");
        assertThat(fake.reads).isEqualTo(4);
    }

    @Test
    void requestWaitOnAFailedRunExitsWithTheReason() {
        fake.latest.add(Optional.of(run("run-1", "FAILED", List.of())));
        assertThat(run("review", "request", "Demo", "Goal", "7", "--wait"))
                .isEqualTo(ExitCode.REQUEST_ERROR);
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("FAILED run run-1: provider timed out");
    }

    @Test
    void requestWaitGivesUpAtTheLimitNamingTheRun() {
        fake.latest.add(Optional.of(run("run-1", "RUNNING", List.of())));
        assertThat(run("review", "request", "Demo", "Goal", "7", "--wait"))
                .isEqualTo(ExitCode.REQUEST_ERROR);
        assertThat(err.toString(StandardCharsets.UTF_8))
                .contains("requel review show Demo Goal 7 (run run-1)");
    }

    @Test
    void anUnavailableReviewPrintsTheReasonAndExitsWithARequestError() {
        fake.refusal = new GatewayException(GatewayException.Kind.CONFLICT,
                "AI review is off on this server.");
        assertThat(run("review", "request", "Demo", "Goal", "7")).isEqualTo(ExitCode.REQUEST_ERROR);
        assertThat(err.toString(StandardCharsets.UTF_8))
                .contains("Error [CONFLICT]: AI review is off on this server.");
    }

    @Test
    void showPrintsTheLatestRunOrNeverReviewed() {
        assertThat(run("review", "show", "Demo", "Goal", "7")).isEqualTo(ExitCode.SUCCESS);
        assertThat(out.toString(StandardCharsets.UTF_8)).contains("Never reviewed.");
        assertThat(fake.lastTaskType).isNull();

        fake.latest.add(Optional.of(run("run-1", "SUCCEEDED", List.of())));
        assertThat(run("review", "show", "Demo", "Goal", "7", "--policy"))
                .isEqualTo(ExitCode.SUCCESS);
        assertThat(fake.lastTaskType).isEqualTo("POLICY_REVIEW");
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("SUCCEEDED run run-1 (0 issues, 0 notes)");
    }

    @Test
    void showJsonPrintsTheRunWithIsoTimes() {
        fake.latest.add(Optional.of(run("run-1", "SUCCEEDED", List.of())));
        assertThat(run("--output", "json", "review", "show", "Demo", "Goal", "7"))
                .isEqualTo(ExitCode.SUCCESS);
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("\"createdAt\" : \"2026-10-30T12:00:00Z\"");
    }

    @Test
    void usageRenders() {
        assertThat(new CommandLine(new RequelCli()).getSubcommands().get("review")
                .getSubcommands().keySet()).containsExactlyInAnyOrder("request", "show");
        assertThat(new CommandLine(new ReviewCommand()).getUsageMessage()).isNotBlank();
    }
}
