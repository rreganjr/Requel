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
package com.rreganjr.requel.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.gateway.rest.RestCommandGateway;
import com.rreganjr.requel.gateway.rest.RestQueryGateway;
import com.rreganjr.requel.gateway.tracker.RequirementGoalUpserter;
import com.rreganjr.requel.gateway.tracker.UpsertGoalRequest;
import com.rreganjr.requel.gateway.tracker.UpsertGoalResult;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * {@code requel upsert-goal} — create or update a project goal from one requirement / acceptance
 * criterion, attaching a machine-parseable provenance note that links it to the source tracker
 * item (issue #71). Orchestrates the REST-backed {@link CommandGateway} (writes) and
 * {@link QueryGateway} (reads) through {@link RequirementGoalUpserter}: it resolves an existing
 * goal by provenance and updates it in place on a re-run, otherwise creates a new goal. Useful for
 * scripting bulk requirement import from any tracker; the client reads the issue, this command
 * hands Requel one discrete statement at a time.
 */
@Command(name = "upsert-goal",
        description = "Create or update a goal from a requirement, with a provenance note linking "
                + "it to a source tracker item.")
public class UpsertGoalCommand implements Callable<Integer> {

    @ParentCommand
    RequelCli parent;

    @Option(names = {"--project", "-p"}, required = true, paramLabel = "NAME",
            description = "Target project name.")
    String project;

    @Option(names = "--criterion", required = true, paramLabel = "TEXT",
            description = "The requirement / acceptance-criterion statement.")
    String criterionText;

    @Option(names = "--source-system", required = true, paramLabel = "SYSTEM",
            description = "Tracker family, e.g. jira, github, linear.")
    String sourceSystem;

    @Option(names = "--source-ref", required = true, paramLabel = "REF",
            description = "Reference to the source item/criterion, e.g. PROJ-123#ac2.")
    String sourceRef;

    @Option(names = "--name", paramLabel = "NAME",
            description = "Explicit goal name (derived from --criterion when omitted).")
    String name;

    @Option(names = "--text", paramLabel = "TEXT",
            description = "Goal body (defaults to --criterion).")
    String text;

    @Option(names = "--source-url", paramLabel = "URL",
            description = "Openable link to the source item.")
    String sourceUrl;

    @Option(names = "--criterion-ref", paramLabel = "REF",
            description = "Human-readable criterion label, e.g. AC-2.")
    String criterionRef;

    @Option(names = "--criterion-hash", paramLabel = "HASH",
            description = "Precomputed reconciliation hash (computed from --criterion when omitted).")
    String criterionHash;

    /** Test seams: when set, used instead of building REST-backed gateways from the parent. */
    CommandGateway commandGatewayOverride;
    QueryGateway queryGatewayOverride;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Integer call() {
        CommandGateway command = commandGatewayOverride != null ? commandGatewayOverride
                : new RestCommandGateway(parent.url, parent.tokenSource());
        QueryGateway query = queryGatewayOverride != null ? queryGatewayOverride
                : new RestQueryGateway(parent.url, parent.tokenSource());

        UpsertGoalRequest request;
        try {
            request = new UpsertGoalRequest(project, criterionText, name, text, sourceSystem,
                    sourceRef, sourceUrl, criterionRef, "requel-cli", criterionHash);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid input: " + e.getMessage());
            return ExitCode.USAGE;
        }

        try {
            UpsertGoalResult result = new RequirementGoalUpserter(command, query).upsert(request);
            print(result);
            return ExitCode.SUCCESS;
        } catch (GatewayException e) {
            parent.printError(e);
            return ExitCode.forKind(e.getKind());
        }
    }

    private void print(UpsertGoalResult result) {
        if (parent.output == OutputFormat.JSON) {
            try {
                System.out.println(mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(result));
            } catch (JsonProcessingException e) {
                System.out.println(String.valueOf(result));
            }
            return;
        }
        System.out.println((result.created() ? "Created" : "Updated") + " goal "
                + result.goalId() + " \"" + result.goalName() + "\" (provenance note "
                + result.noteId() + ")");
    }
}
