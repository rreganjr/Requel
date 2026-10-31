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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rreganjr.requel.gateway.AiReviewGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.QueryDescriptions;
import com.rreganjr.requel.gateway.rest.RestAiReviewGateway;
import com.rreganjr.requel.service.api.dto.AiReviewDto;
import com.rreganjr.requel.service.api.dto.AiReviewRequestDto;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Issue #387: {@code requel review request|show PROJECT TYPE ID} - ask the server's AI assistant to
 * review one entity, and read the latest review. Both go through
 * {@code /api/gateway/ai/reviews}, with the editor button's rules: access to the project, and a
 * request is refused (with the reason) when AI review can't run for the entity, or when the
 * server has gateway writes disabled.
 */
@Command(name = "review", description = "Request and read an entity's AI review.",
        subcommands = {ReviewCommand.Request.class, ReviewCommand.Show.class})
public class ReviewCommand implements Runnable {

    @ParentCommand
    RequelCli parent;

    /** Test seam: when set, used instead of building a {@link RestAiReviewGateway}. */
    AiReviewGateway gatewayOverride;

    /** How often {@code --wait} reads the run, and how long it waits. Package-visible for tests. */
    Duration pollInterval = Duration.ofSeconds(3);
    Duration waitLimit = Duration.ofMinutes(5);

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    AiReviewGateway gateway() {
        return gatewayOverride != null ? gatewayOverride
                : new RestAiReviewGateway(parent.url, parent.tokenSource());
    }

    /** The entity: the same three positionals for both subcommands. */
    static class Target {
        @Parameters(index = "0", paramLabel = "PROJECT", description = QueryDescriptions.PROJECT_NAME)
        String projectName;

        @Parameters(index = "1", paramLabel = "TYPE",
                description = QueryDescriptions.REVIEWABLE_ENTITY_TYPE)
        String entityType;

        @Parameters(index = "2", paramLabel = "ID", description = QueryDescriptions.ENTITY_ID)
        long entityId;
    }

    @Command(name = "request", description = QueryDescriptions.REQUEST_AI_REVIEW)
    static class Request implements Callable<Integer> {

        @ParentCommand
        ReviewCommand review;

        @CommandLine.Mixin
        Target target = new Target();

        @Option(names = "--wait", description = "Wait for the review to finish (checking every"
                + " 3 seconds, for up to 5 minutes) and print it.")
        boolean waitForIt;

        @Override
        public Integer call() {
            AiReviewGateway gateway = review.gateway();
            try {
                AiReviewRequestDto runs = gateway.requestReview(target.projectName,
                        target.entityType, target.entityId);
                if (!waitForIt) {
                    review.print(runs, "Review queued: run " + runs.runId()
                            + (runs.policyRunId() == null ? ""
                                    : " (policy pass: run " + runs.policyRunId() + ")"));
                    return ExitCode.SUCCESS;
                }
                return review.waitFor(gateway, target, runs.runId());
            } catch (GatewayException e) {
                review.parent.printError(e);
                return ExitCode.forKind(e.getKind());
            }
        }
    }

    @Command(name = "show", description = QueryDescriptions.GET_AI_REVIEW)
    static class Show implements Callable<Integer> {

        @ParentCommand
        ReviewCommand review;

        @CommandLine.Mixin
        Target target = new Target();

        @Option(names = "--policy", description = "Read the project policies' pass instead.")
        boolean policy;

        @Override
        public Integer call() {
            try {
                Optional<AiReviewDto> run = review.gateway().latestReview(target.projectName,
                        target.entityType, target.entityId,
                        policy ? AiReviewGateway.POLICY_REVIEW : null);
                if (run.isEmpty()) {
                    review.print(null, "Never reviewed.");
                    return ExitCode.SUCCESS;
                }
                review.print(run.get(), render(run.get()));
                return ExitCode.SUCCESS;
            } catch (GatewayException e) {
                review.parent.printError(e);
                return ExitCode.forKind(e.getKind());
            }
        }
    }

    /**
     * Read the run until it is SUCCEEDED or FAILED, then print it. A FAILED run, or one still
     * unfinished at the limit, exits with {@link ExitCode#REQUEST_ERROR}.
     */
    int waitFor(AiReviewGateway gateway, Target target, String runId) throws GatewayException {
        long deadline = System.nanoTime() + waitLimit.toNanos();
        while (true) {
            Optional<AiReviewDto> run = gateway.latestReview(target.projectName,
                    target.entityType, target.entityId, null);
            if (run.isPresent() && runId.equals(run.get().runId()) && run.get().finished()) {
                print(run.get(), render(run.get()));
                return "SUCCEEDED".equals(run.get().status()) ? ExitCode.SUCCESS
                        : ExitCode.REQUEST_ERROR;
            }
            if (System.nanoTime() >= deadline) {
                System.err.println("Still running after " + waitLimit.toMinutes() + " minutes;"
                        + " check later with: requel review show " + target.projectName + " "
                        + target.entityType + " " + target.entityId + " (run " + runId + ")");
                return ExitCode.REQUEST_ERROR;
            }
            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ExitCode.UNEXPECTED;
            }
        }
    }

    /** {@code --output json} prints the value; text prints {@code text}. */
    void print(Object value, String text) {
        if (parent.output == OutputFormat.JSON) {
            try {
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value));
            } catch (JsonProcessingException e) {
                System.out.println(String.valueOf(value));
            }
        } else {
            System.out.println(text);
        }
    }

    /**
     * A run as text: its status and counts, the model's summary, then one line per finding,
     * severity first as {@code open-issues} prints them.
     */
    static String render(AiReviewDto run) {
        StringBuilder sb = new StringBuilder(run.status()).append(" run ").append(run.runId());
        if ("FAILED".equals(run.status())) {
            return sb.append(": ").append(run.errorSummary() == null ? "no reason given"
                    : run.errorSummary()).toString();
        }
        if (!run.finished()) {
            return sb.toString();
        }
        long issues = run.findings().stream().filter(f -> "ISSUE".equals(f.kind())).count();
        long notes = run.findings().size() - issues;
        sb.append(" (").append(count(issues, "issue")).append(", ").append(count(notes, "note"))
                .append(')');
        if (run.summary() != null && !run.summary().isBlank()) {
            sb.append('\n').append(run.summary().strip());
        }
        for (AiReviewDto.Finding f : run.findings()) {
            sb.append('\n').append(String.format("%-7s%-6s", f.severity() == null ? "" : f.severity(),
                    f.kind())).append(f.text());
        }
        return sb.toString();
    }

    private static String count(long n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }
}
